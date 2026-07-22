package by.taverna.shlyapnika.internal;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.config.TavernaProperties;
import by.taverna.shlyapnika.internal.api.InternalBotSessionRequest;
import by.taverna.shlyapnika.internal.api.InternalBotSessionResponse;
import by.taverna.shlyapnika.internal.api.InternalGameRequest;
import by.taverna.shlyapnika.internal.api.InternalGameUpdateRequest;
import by.taverna.shlyapnika.internal.api.InternalMasterRequest;
import by.taverna.shlyapnika.internal.api.InternalMasterResponse;
import by.taverna.shlyapnika.master.domain.MasterEntity;
import by.taverna.shlyapnika.master.infrastructure.MasterRepository;
import by.taverna.shlyapnika.schedule.ScheduleService;
import by.taverna.shlyapnika.schedule.api.GameResponses.PublicGameDto;
import by.taverna.shlyapnika.schedule.domain.GameEntity;
import by.taverna.shlyapnika.schedule.infrastructure.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalService {
  private static final Set<String> GAME_STATUSES = Set.of("draft", "pending", "published", "completed", "cancelled", "archived");

  private final MasterRepository masters;
  private final GameRepository games;
  private final ScheduleService schedule;
  private final TavernaProperties properties;
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public InternalService(
      MasterRepository masters,
      GameRepository games,
      ScheduleService schedule,
      TavernaProperties properties,
      JdbcTemplate jdbcTemplate,
      AuditService auditService,
      ObjectMapper objectMapper
  ) {
    this.masters = masters;
    this.games = games;
    this.schedule = schedule;
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public InternalMasterResponse getMasterByTelegram(Long telegramUserId) {
    return masters.findByTelegramUserId(telegramUserId)
        .map(this::toMasterResponse)
        .orElseThrow(() -> new NotFoundException("Мастер не найден."));
  }

  @Transactional
  public InternalMasterResponse upsertMaster(InternalMasterRequest request) {
    var telegramUsername = trimToNull(request.telegramUsername());
    var displayName = request.displayName().trim();
    var contactUrl = request.contactUrl().trim();
    var master = masters.findByTelegramUserId(request.telegramUserId())
        .map(existing -> {
          existing.updateProfile(telegramUsername, displayName, contactUrl);
          return existing;
        })
        .orElseGet(() -> MasterEntity.create(request.telegramUserId(), telegramUsername, displayName, contactUrl));
    master = masters.save(master);
    auditService.write(String.valueOf(request.telegramUserId()), "master.upserted", "Master", master.getId(), null);
    return toMasterResponse(master);
  }

  @Transactional(readOnly = true)
  public InternalBotSessionResponse getBotSession(Long telegramUserId) {
    var rows = jdbcTemplate.query(
        "select \"telegramUserId\", \"state\", \"draft\"::text as \"draft\" from \"BotSession\" where \"telegramUserId\" = ?",
        (rs, row) -> new InternalBotSessionResponse(
            rs.getLong("telegramUserId"),
            rs.getString("state"),
            parseDraft(rs.getString("draft"))
        ),
        telegramUserId
    );
    return rows.stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Сессия бота не найдена."));
  }

  @Transactional
  public InternalBotSessionResponse saveBotSession(Long telegramUserId, InternalBotSessionRequest request) {
    var draft = request.draft() == null || request.draft().isNull() ? objectMapper.createObjectNode() : request.draft();
    jdbcTemplate.update(
        """
        insert into "BotSession" ("telegramUserId", "state", "draft", "createdAt", "updatedAt")
        values (?, ?, ?::jsonb, current_timestamp, current_timestamp)
        on conflict ("telegramUserId") do update
        set "state" = excluded."state",
            "draft" = excluded."draft",
            "updatedAt" = current_timestamp
        """,
        telegramUserId,
        request.state(),
        draft.toString()
    );
    return new InternalBotSessionResponse(telegramUserId, request.state(), draft);
  }

  @Transactional
  public void deleteBotSession(Long telegramUserId) {
    jdbcTemplate.update("delete from \"BotSession\" where \"telegramUserId\" = ?", telegramUserId);
  }

  @Transactional
  public PublicGameDto createGame(InternalGameRequest request) {
    if (request.minPlayers() > request.maxPlayers()) {
      throw new IllegalArgumentException("Минимум игроков не может быть больше максимума.");
    }
    var master = masters.findById(request.masterId()).orElseThrow(() -> new NotFoundException("Мастер не найден."));
    var zone = ZoneId.of(properties.timezone());
    var start = LocalDate.parse(request.date()).atTime(LocalTime.parse(request.time())).atZone(zone).toInstant();
    var game = games.save(GameEntity.create(
        master,
        request.title().trim(),
        request.description().trim(),
        request.gameSystem().trim(),
        request.experienceLevel().trim(),
        request.ageRating().trim(),
        start,
        request.durationMinutes(),
        request.minPlayers(),
        request.maxPlayers(),
        request.price(),
        request.currency(),
        request.contactUrl().trim(),
        properties.autoPublish()
    ));
    auditService.write(String.valueOf(master.getTelegramUserId()), "game.created", "Game", game.getId(), "{\"status\":\"" + game.getStatus() + "\"}");
    return schedule.toDto(game);
  }

  @Transactional
  public PublicGameDto setGameStatus(String id, String status) {
    if (!GAME_STATUSES.contains(status)) throw new IllegalArgumentException("Недопустимый статус игры.");
    var game = games.findById(id).orElseThrow(() -> new NotFoundException("Игра не найдена."));
    game.setStatus(status);
    games.save(game);
    auditService.write(null, "game.status_" + status, "Game", id, null);
    return schedule.toDto(game);
  }

  @Transactional(readOnly = true)
  public java.util.List<PublicGameDto> listMasterGames(String masterId, String scope) {
    var now = Instant.now();
    return games.findByMasterIdForBot(masterId).stream()
        .filter(game -> switch (scope == null ? "all" : scope) {
          case "upcoming" -> Set.of("published", "pending").contains(game.getStatus()) && !game.getDateTimeStart().isBefore(now);
          case "past" -> game.getDateTimeStart().isBefore(now) || Set.of("completed", "cancelled", "archived").contains(game.getStatus());
          default -> true;
        })
        .limit(20)
        .map(schedule::toDto)
        .toList();
  }

  @Transactional
  public PublicGameDto setMasterGameStatus(String masterId, String gameId, String status) {
    if (!GAME_STATUSES.contains(status)) throw new IllegalArgumentException("Недопустимый статус игры.");
    var game = games.findByIdAndMasterIdForBot(gameId, masterId)
        .orElseThrow(() -> new NotFoundException("Игра не найдена или принадлежит другому мастеру."));
    game.setStatus(status);
    games.save(game);
    auditService.write(String.valueOf(game.getMaster().getTelegramUserId()), "game.status_" + status, "Game", gameId, null);
    return schedule.toDto(game);
  }

  @Transactional
  public PublicGameDto updateMasterGame(String masterId, String gameId, InternalGameUpdateRequest request) {
    var game = games.findByIdAndMasterIdForBot(gameId, masterId)
        .orElseThrow(() -> new NotFoundException("Игра не найдена или принадлежит другому мастеру."));

    if (request.title() != null) game.updateTitle(request.title().trim());
    if (request.description() != null) game.updateDescription(request.description().trim());
    if (request.gameSystem() != null) game.updateGameSystem(request.gameSystem().trim());
    if (request.experienceLevel() != null) game.updateExperienceLevel(request.experienceLevel().trim());
    if (request.ageRating() != null) game.updateAgeRating(request.ageRating().trim());
    if (request.date() != null || request.time() != null) {
      if (request.date() == null || request.time() == null) throw new IllegalArgumentException("Для изменения времени укажите дату и время.");
      var start = LocalDate.parse(request.date()).atTime(LocalTime.parse(request.time())).atZone(ZoneId.of(properties.timezone())).toInstant();
      game.updateDateTimeStart(start);
    }
    if (request.durationMinutes() != null) game.updateDurationMinutes(request.durationMinutes());
    if (request.minPlayers() != null || request.maxPlayers() != null) {
      if (request.minPlayers() == null || request.maxPlayers() == null) throw new IllegalArgumentException("Для изменения игроков укажите минимум и максимум.");
      if (request.minPlayers() > request.maxPlayers()) throw new IllegalArgumentException("Минимум игроков не может быть больше максимума.");
      game.updatePlayers(request.minPlayers(), request.maxPlayers());
    }
    if (request.price() != null) game.updatePrice(request.price(), request.currency());
    if (request.contactUrl() != null) game.updateContactUrl(request.contactUrl().trim());

    games.save(game);
    auditService.write(String.valueOf(game.getMaster().getTelegramUserId()), "game.updated", "Game", gameId, null);
    return schedule.toDto(game);
  }

  public int archivePastGames() {
    return schedule.archivePastGames();
  }

  @Transactional
  public int withdrawConsent(String entityType, String requestId, boolean anonymize) {
    var table = switch (entityType) {
      case "GameSignup" -> "\"GameSignup\"";
      case "ServiceRequest" -> "\"ServiceRequest\"";
      case "ContactRequest" -> "\"ContactRequest\"";
      default -> throw new IllegalArgumentException("Неизвестный тип заявки.");
    };
    var updated = jdbcTemplate.update(
        "update " + table + " set \"consentWithdrawnAt\" = current_timestamp, \"dataUseStoppedAt\" = current_timestamp, \"updatedAt\" = current_timestamp where \"id\" = ?",
        requestId
    );
    if (anonymize && updated > 0) {
      jdbcTemplate.update(
          "update " + table + " set \"name\" = '[удалено]', \"contact\" = '[удалено]', \"comment\" = null, \"anonymizedAt\" = current_timestamp where \"id\" = ?",
          requestId
      );
    }
    auditService.write(null, "privacy.withdraw_consent", entityType, requestId, "{\"anonymize\":" + anonymize + "}");
    return updated;
  }

  private InternalMasterResponse toMasterResponse(MasterEntity master) {
    return new InternalMasterResponse(
        master.getId(),
        master.getTelegramUserId(),
        master.getTelegramUsername(),
        master.getDisplayName(),
        master.getContactUrl(),
        master.getRole(),
        master.getStatus()
    );
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private JsonNode parseDraft(String value) {
    try {
      return value == null || value.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(value);
    } catch (Exception error) {
      return objectMapper.createObjectNode();
    }
  }
}
