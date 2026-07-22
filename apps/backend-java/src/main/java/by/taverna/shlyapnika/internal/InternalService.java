package by.taverna.shlyapnika.internal;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.config.TavernaProperties;
import by.taverna.shlyapnika.internal.api.InternalGameRequest;
import by.taverna.shlyapnika.master.infrastructure.MasterRepository;
import by.taverna.shlyapnika.schedule.api.GameResponses.PublicGameDto;
import by.taverna.shlyapnika.schedule.ScheduleService;
import by.taverna.shlyapnika.schedule.domain.GameEntity;
import by.taverna.shlyapnika.schedule.infrastructure.GameRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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

  public InternalService(
      MasterRepository masters,
      GameRepository games,
      ScheduleService schedule,
      TavernaProperties properties,
      JdbcTemplate jdbcTemplate,
      AuditService auditService
  ) {
    this.masters = masters;
    this.games = games;
    this.schedule = schedule;
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
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
    auditService.write(null, "game.created", "Game", game.getId(), "{\"status\":\"" + game.getStatus() + "\"}");
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
}
