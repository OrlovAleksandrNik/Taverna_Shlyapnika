package by.taverna.shlyapnika.schedule;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.consent.ConsentService;
import by.taverna.shlyapnika.config.TavernaProperties;
import by.taverna.shlyapnika.notification.TelegramNotificationService;
import by.taverna.shlyapnika.schedule.api.GameResponses.MasterDto;
import by.taverna.shlyapnika.schedule.api.GameResponses.PublicGameDto;
import by.taverna.shlyapnika.schedule.api.GameSignupRequest;
import by.taverna.shlyapnika.schedule.domain.GameEntity;
import by.taverna.shlyapnika.schedule.domain.GameSignupEntity;
import by.taverna.shlyapnika.schedule.infrastructure.GameRepository;
import by.taverna.shlyapnika.schedule.infrastructure.GameSignupRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
  private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

  private final GameRepository games;
  private final GameSignupRepository signups;
  private final ConsentService consentService;
  private final AuditService auditService;
  private final TavernaProperties properties;
  private final TelegramNotificationService notifications;

  public ScheduleService(
      GameRepository games,
      GameSignupRepository signups,
      ConsentService consentService,
      AuditService auditService,
      TavernaProperties properties,
      TelegramNotificationService notifications
  ) {
    this.games = games;
    this.signups = signups;
    this.consentService = consentService;
    this.auditService = auditService;
    this.properties = properties;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public List<PublicGameDto> listPublicGames(Instant dateFrom, Instant dateTo, String masterId, String system, Integer limit, Integer offset) {
    var now = Instant.now();
    var from = dateFrom != null && dateFrom.isAfter(now) ? dateFrom : now;
    var safeLimit = Math.min(limit == null ? 30 : limit, 100);
    var safeOffset = Math.max(offset == null ? 0 : offset, 0);
    var normalizedSystem = system == null ? "" : system.toLowerCase(Locale.ROOT);
    var result = games.findPublicGames("published", from).stream()
        .filter(game -> dateTo == null || !game.getDateTimeStart().isAfter(dateTo))
        .filter(game -> masterId == null || masterId.isBlank() || game.getMaster().getId().equals(masterId))
        .filter(game -> normalizedSystem.isBlank() || game.getGameSystem().toLowerCase(Locale.ROOT).contains(normalizedSystem))
        .skip(safeOffset)
        .limit(safeLimit)
        .map(this::toDto)
        .toList();
    log.info("public games requested count={}", result.size());
    return result;
  }

  @Transactional(readOnly = true)
  public List<PublicGameDto> listPublicGames() {
    return listPublicGames(null, null, null, null, null, null);
  }

  @Transactional(readOnly = true)
  public PublicGameDto getPublicGame(String id) {
    return games.findPublicGame(id, "published", Instant.now()).map(this::toDto)
        .orElseThrow(() -> new NotFoundException("Игра не найдена."));
  }

  @Transactional
  public GameSignupResult createSignup(GameSignupRequest request) {
    var game = games.findPublicGame(request.gameId(), "published", Instant.now())
        .orElseThrow(() -> new NotFoundException("Игра не найдена или уже недоступна для записи."));
    var seats = request.seats() == null ? 1 : request.seats();
    var bookedSeats = signups.confirmedSeats(game.getId());
    if (bookedSeats + seats > game.getMaxPlayers()) {
      throw new IllegalArgumentException("На эту игру уже нет нужного количества свободных мест.");
    }

    var consent = consentService.require(
        request.consentGiven(),
        request.consentVersion(),
        request.privacyPolicyVersion(),
        "game-booking"
    );
    var existing = signups.findByGame_IdAndContact(game.getId(), request.contact()).orElse(null);
    var signup = existing == null
        ? GameSignupEntity.confirmed(game, request.playerName(), request.contact(), seats, emptyToNull(request.comment()), consent)
        : existing;
    if (existing != null) {
      signup.updateConfirmed(request.playerName(), seats, emptyToNull(request.comment()), consent);
    }
    signup = signups.save(signup);
    auditService.write(null, "game.signup", "GameSignup", signup.getId(), "{\"gameId\":\"" + game.getId() + "\"}");
    notifySignup(game, request.playerName(), request.contact(), seats, request.comment());
    log.info("game signup saved signupId={} gameId={}", signup.getId(), game.getId());
    return new GameSignupResult(signup.getId(), toDto(game));
  }

  @Scheduled(fixedDelayString = "${taverna.archive-delay-ms:900000}")
  @Transactional
  public void archivePastGamesScheduled() {
    archivePastGames();
  }

  @Transactional
  public int archivePastGames() {
    var now = Instant.now();
    var archived = games.findPublicGames("published", Instant.EPOCH).stream()
        .filter(game -> gameEnd(game).isBefore(now))
        .peek(game -> game.setStatus("archived"))
        .toList();
    games.saveAll(archived);
    if (!archived.isEmpty()) log.info("past games archived count={}", archived.size());
    return archived.size();
  }

  private Instant gameEnd(GameEntity game) {
    if (game.getDateTimeEnd() != null) return game.getDateTimeEnd();
    var minutes = game.getDurationMinutes() == null ? 360 : game.getDurationMinutes();
    return game.getDateTimeStart().plusSeconds((long) minutes * 60);
  }

  public PublicGameDto toDto(GameEntity game) {
    var booked = signups.confirmedSeats(game.getId());
    var master = game.getMaster();
    return new PublicGameDto(
        game.getId(),
        game.getTitle(),
        game.getDescription(),
        game.getGameSystem(),
        game.getGameSystem(),
        game.getGameSystem(),
        game.getExperienceLevel(),
        game.getAgeRating(),
        game.getDateTimeStart(),
        startsAtLabel(game),
        game.getDurationMinutes(),
        game.getDateTimeEnd(),
        game.getMinPlayers(),
        game.getMaxPlayers(),
        game.getPrice(),
        game.getCurrency(),
        game.getImageUrl(),
        game.getContactUrl(),
        game.getStatus(),
        master.getId(),
        master.getDisplayName(),
        master.getTelegramUsername(),
        new MasterDto(master.getId(), master.getDisplayName(), master.getContactUrl(), master.getTelegramUsername()),
        booked,
        Math.max(0, game.getMaxPlayers() - booked),
        tags(game)
    );
  }

  private String startsAtLabel(GameEntity game) {
    var formatter = DateTimeFormatter.ofPattern("d MMMM, EEEE HH:mm", Locale.forLanguageTag("ru-RU"));
    return formatter.format(game.getDateTimeStart().atZone(ZoneId.of(properties.timezone())));
  }

  private List<String> tags(GameEntity game) {
    return List.of(isDndSystem(game.getGameSystem()) ? "dnd" : "other", isNewbieExperience(game.getExperienceLevel()) ? "newbie" : "experienced");
  }

  private boolean isDndSystem(String value) {
    var normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    return normalized.contains("d&d") || normalized.contains("dnd") || normalized.contains("dungeons");
  }

  private boolean isNewbieExperience(String value) {
    var normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    return normalized.contains("нов") || normalized.contains("без") || normalized.contains("нач");
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private void notifySignup(GameEntity game, String playerName, String contact, int seats, String comment) {
    var message = String.join("\n",
        "Новая запись на игру",
        "",
        "Игра: " + game.getTitle(),
        "Дата: " + startsAtLabel(game),
        "Игрок: " + playerName,
        "Контакт: " + contact,
        "Мест: " + seats,
        emptyToNull(comment) == null ? "" : "Комментарий: " + comment
    ).trim();
    notifications.notifyTelegram(game.getMaster().getTelegramUserId(), message);
    notifications.notifyAdmins(message);
  }

  public record GameSignupResult(String signupId, PublicGameDto game) {
  }
}
