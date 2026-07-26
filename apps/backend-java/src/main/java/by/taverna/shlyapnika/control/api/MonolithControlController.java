package by.taverna.shlyapnika.control.api;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.config.TavernaProperties;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonolithControlController {
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;
  private final TavernaProperties properties;

  public MonolithControlController(JdbcTemplate jdbcTemplate, AuditService auditService, TavernaProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
    this.properties = properties;
  }

  @GetMapping("/api/v1/admin/dashboard")
  public DashboardResponse dashboard() {
    // Кабинет пока читает только безопасные сводки, без контактов игроков и клиентов.
    var metrics = List.of(
        new MetricDto("Ближайшие игры", count("""
            select count(*) from "Game"
            where "dateTimeStart" >= current_timestamp
              and "status" not in ('cancelled', 'archived', 'completed')
            """), "основная база"),
        new MetricDto("Новые заявки", count("""
            select count(*) from "ServiceRequest"
            where "status" = 'new'
            """), "без контактов"),
        new MetricDto("Активные мастера", count("""
            select count(*) from "Master"
            where "status" = 'active'
            """), "профили"),
        new MetricDto("Публикации галереи", count("""
            select count(*) from "GalleryPost"
            where "status" = 'published'
              and "isVisible" = true
            """), "галерея"),
        new MetricDto("Игроки рейтинга", count("""
            select count(*) from "RatingPlayer"
            where "isVisible" = true
            """), "рейтинг"),
        new MetricDto("Записи на игры", count("""
            select count(*) from "GameSignup"
            where "status" = 'confirmed'
            """), "подтверждены")
    );
    return new DashboardResponse(
        "monolith",
        Instant.now(),
        metrics,
        upcomingGames(8),
        recentActions(8)
    );
  }

  @GetMapping("/api/v1/admin/games")
  public ItemsResponse<GameRowDto> games(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return new ItemsResponse<>(gameRows(false, page, size), page, size);
  }

  @PostMapping("/api/v1/admin/games")
  @ResponseStatus(HttpStatus.CREATED)
  public GameRowDto createGame(@RequestBody AdminGameRequest request) {
    var master = findMaster(request.masterPublicId());
    var id = Ids.newId("gm");
    var startsAt = Instant.parse(required(request.startsAt(), "startsAt"));
    var durationMinutes = positiveOrDefault(request.durationMinutes(), 180);
    var endsAt = startsAt.plusSeconds(durationMinutes.longValue() * 60);
    var status = "draft";
    jdbcTemplate.update("""
        insert into "Game" (
          "id", "masterId", "title", "description", "gameSystem", "experienceLevel", "ageRating",
          "dateTimeStart", "durationMinutes", "dateTimeEnd", "minPlayers", "maxPlayers",
          "price", "currency", "imageUrl", "contactUrl", "status", "createdAt", "updatedAt"
        )
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null, ?, ?::"GameStatus", current_timestamp, current_timestamp)
        """,
        id,
        master.id(),
        textOrDefault(request.title(), "Новая игра"),
        textOrDefault(request.description(), "Описание будет добавлено мастером."),
        textOrDefault(request.gameSystem(), "D&D 5e"),
        textOrDefault(request.experienceLevel(), "любой уровень"),
        textOrDefault(request.ageRating(), "12+"),
        Timestamp.from(startsAt),
        durationMinutes,
        Timestamp.from(endsAt),
        positiveOrDefault(request.minPlayers(), 1),
        positiveOrDefault(request.maxPlayers(), 5),
        request.price() == null ? BigDecimal.ZERO : request.price(),
        textOrDefault(request.currency(), "BYN"),
        textOrDefault(request.contactUrl(), master.contactUrl()),
        status
    );
    auditService.write("master-cabinet", "game.created_from_cabinet", "Game", id, "{\"status\":\"draft\"}");
    return new GameRowDto(id, textOrDefault(request.title(), "Новая игра"), startsAt, master.id(), master.displayName(), textOrDefault(request.gameSystem(), "D&D 5e"), status);
  }

  @PostMapping("/api/v1/admin/games/{id}/publish")
  public GameRowDto publishGame(@PathVariable String id) {
    return setGameStatus(id, "published", "game.published_from_cabinet");
  }

  @PostMapping("/api/v1/admin/games/{id}/cancel")
  public GameRowDto cancelGame(@PathVariable String id) {
    return setGameStatus(id, "cancelled", "game.cancelled_from_cabinet");
  }

  @DeleteMapping("/api/v1/admin/games/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archiveGame(@PathVariable String id) {
    setGameStatus(id, "archived", "game.archived_from_cabinet");
  }

  @GetMapping("/api/v1/admin/schedule")
  public ItemsResponse<GameRowDto> schedule(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to
  ) {
    return new ItemsResponse<>(gameRows(true, 0, 100), 0, 100);
  }

  @GetMapping("/api/v1/admin/data/{section}")
  public ItemsResponse<ControlRecordDto> data(
      @PathVariable String section,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var offset = safePage * safeSize;
    var items = switch (section) {
      case "applications", "services" -> serviceRequests(safeSize, offset);
      case "masters" -> masters(safeSize, offset);
      case "players", "rating" -> ratingPlayers(safeSize, offset);
      case "gallery" -> galleryPosts(null, safeSize, offset);
      case "stories" -> galleryPosts("story", safeSize, offset);
      case "notifications" -> recentNotificationLikeActions(safeSize);
      default -> List.<ControlRecordDto>of();
    };
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/audit")
  public ItemsResponse<ActionDto> audit(@RequestParam(defaultValue = "0") int page) {
    return new ItemsResponse<>(recentActions(30), Math.max(page, 0), 30);
  }

  @GetMapping("/api/v1/admin/projects")
  public List<ProjectDto> projects() {
    return List.of(
        new ProjectDto("site-monolith", "Основной сайт и API", "Java 21 Spring Boot + static frontend", "apps/backend-java", "active", "monolith"),
        new ProjectDto("telegram-bot", "Писарь таверны", "Java Telegram Bot + internal API", "apps/telegram-bot-java", "active", "monolith"),
        new ProjectDto("master-cabinet", "Кабинет мастера", "Vite frontend inside monolith", "apps/master-cabinet", "active", "monolith")
    );
  }

  @GetMapping("/api/v1/admin/users")
  public ItemsResponse<AccountDto> users(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var items = jdbcTemplate.query("""
        select "id", "telegramUsername", "displayName", "role", "status"
        from "Master"
        order by "displayName" asc
        limit ? offset ?
        """, (rs, rowNum) -> new AccountDto(
            rs.getString("id"),
            publicTelegramHandle(rs.getString("telegramUsername")),
            List.of(rs.getString("role")),
            rs.getString("status")
        ), safeSize, safePage * safeSize);
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/rating/players")
  public ItemsResponse<RatingPlayerRowDto> ratingPlayerRows(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var offset = safePage * safeSize;
    var items = jdbcTemplate.query("""
        select "id", "displayName", "nickname", "isVisible", "gamesPlayed",
               "totalPoints", "inspirationCount",
               round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
               "lastGameAt", "lastStatsAt", "updatedAt"
        from "RatingPlayer"
        order by "totalPoints" desc,
                 case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
                 "gamesPlayed" desc,
                 lower("displayName") asc
        limit ? offset ?
        """, (rs, rowNum) -> new RatingPlayerRowDto(
            offset + rowNum + 1,
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("nickname"),
            rs.getBoolean("isVisible"),
            rs.getInt("gamesPlayed"),
            rs.getInt("totalPoints"),
            rs.getInt("inspirationCount"),
            rs.getBigDecimal("averagePointsPerGame"),
            instant(rs, "lastGameAt"),
            instant(rs, "lastStatsAt"),
            instant(rs, "updatedAt")
        ), safeSize, offset);
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/files/storage")
  public StorageDto filesStorage() {
    return new StorageDto(
        new StorageAdapterDto("local", properties.fileStorageDir()),
        new StorageAdapterDto("disabled", "project artifacts are not enabled in monolith yet"),
        List.of("s3-compatible", "railway-volume")
    );
  }

  @GetMapping("/api/v1/admin/backups")
  public ItemsResponse<BackupJobDto> backups() {
    return new ItemsResponse<>(List.of(
        new BackupJobDto("backup-read-only", "DISABLED", "manual database backups required", "not configured")
    ), 0, 20);
  }

  @GetMapping("/api/v1/admin/integration/status")
  public Map<String, Object> integrationStatus() {
    return Map.of(
        "mode", "monolith",
        "mainSiteIntegrationEnabled", true,
        "telegramIntegrationEnabled", true,
        "desktopAgentEnabled", false,
        "contractsPrepared", true,
        "productionDataUsed", true
    );
  }

  @GetMapping("/api/v1/admin/settings")
  public Map<String, Object> settings() {
    return Map.of(
        "featureFlags", Map.of(
            "publicRegistration", false,
            "mainSiteIntegration", true,
            "telegramIntegration", true,
            "desktopAgent", false
        ),
        "storedSettings", List.of(
            new ControlRecordDto("monolith", "Режим проекта", "active", Instant.now()),
            new ControlRecordDto("master-cabinet", "Кабинет мастера", "active", Instant.now())
        )
    );
  }

  private List<GameRowDto> gameRows(boolean onlyFuture, int page, int size) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var dateFilter = onlyFuture ? "where g.\"dateTimeStart\" >= current_timestamp" : "";
    return jdbcTemplate.query("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        %s
        order by g."dateTimeStart" asc
        limit ? offset ?
        """.formatted(dateFilter), (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), safeSize, safePage * safeSize);
  }

  private List<GameRowDto> upcomingGames(int limit) {
    return jdbcTemplate.query("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        where g."dateTimeStart" >= current_timestamp
          and g."status" not in ('cancelled', 'archived', 'completed')
        order by g."dateTimeStart" asc
        limit ?
        """, (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), limit);
  }

  private List<ControlRecordDto> serviceRequests(int limit, int offset) {
    return jdbcTemplate.query("""
        select "id", "service", "status", "updatedAt"
        from "ServiceRequest"
        order by "createdAt" desc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("service"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), limit, offset);
  }

  private List<ControlRecordDto> masters(int limit, int offset) {
    return jdbcTemplate.query("""
        select "id", "displayName", "status", "updatedAt"
        from "Master"
        order by "displayName" asc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), limit, offset);
  }

  private List<ControlRecordDto> ratingPlayers(int limit, int offset) {
    return jdbcTemplate.query("""
        select "id", "displayName", "isVisible", "updatedAt"
        from "RatingPlayer"
        order by "totalPoints" desc, "gamesPlayed" desc, lower("displayName") asc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getBoolean("isVisible") ? "published" : "hidden",
            instant(rs, "updatedAt")
        ), limit, offset);
  }

  private List<ControlRecordDto> galleryPosts(String type, int limit, int offset) {
    return jdbcTemplate.query("""
        select "publicId", "title", "status", "updatedAt"
        from "GalleryPost"
        where (?::text is null or "type" = cast(? as "GalleryPostType"))
        order by "createdAt" desc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("publicId"),
            rs.getString("title"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), type, type, limit, offset);
  }

  private List<ControlRecordDto> recentNotificationLikeActions(int limit) {
    return jdbcTemplate.query("""
        select "id", "action", "entityType", "createdAt"
        from "AuditLog"
        order by "createdAt" desc
        limit ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("action"),
            rs.getString("entityType"),
            instant(rs, "createdAt")
        ), limit);
  }

  private List<ActionDto> recentActions(int limit) {
    return jdbcTemplate.query("""
        select "userId", "action", "entityType", "createdAt"
        from "AuditLog"
        order by "createdAt" desc
        limit ?
        """, (rs, rowNum) -> new ActionDto(
            blankToSystem(rs.getString("userId")),
            rs.getString("action"),
            rs.getString("entityType"),
            instant(rs, "createdAt")
        ), limit);
  }

  private int count(String sql) {
    var value = jdbcTemplate.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private GameRowDto setGameStatus(String id, String status, String action) {
    jdbcTemplate.update("""
        update "Game"
        set "status" = ?::"GameStatus",
            "publishedAt" = case when ? = 'published' and "publishedAt" is null then current_timestamp else "publishedAt" end,
            "cancelledAt" = case when ? = 'cancelled' then current_timestamp else "cancelledAt" end,
            "completedAt" = case when ? in ('completed', 'archived') then current_timestamp else "completedAt" end,
            "updatedAt" = current_timestamp
        where "id" = ?
        """, status, status, status, status, id);
    auditService.write("master-cabinet", action, "Game", id, "{\"status\":\"" + status + "\"}");
    return gameById(id);
  }

  private GameRowDto gameById(String id) {
    return jdbcTemplate.queryForObject("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        where g."id" = ?
        """, (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), id);
  }

  private MasterOptionDto findMaster(String masterId) {
    var requestedId = masterId == null || masterId.isBlank() ? null : masterId.trim();
    var rows = requestedId == null
        ? jdbcTemplate.query("""
            select "id", "displayName", "contactUrl"
            from "Master"
            where "status" = 'active'
            order by "displayName" asc
            limit 1
            """, (rs, rowNum) -> new MasterOptionDto(rs.getString("id"), rs.getString("displayName"), rs.getString("contactUrl")))
        : jdbcTemplate.query("""
            select "id", "displayName", "contactUrl"
            from "Master"
            where "id" = ?
            limit 1
            """, (rs, rowNum) -> new MasterOptionDto(rs.getString("id"), rs.getString("displayName"), rs.getString("contactUrl")), requestedId);
    if (rows.isEmpty()) throw new IllegalArgumentException("Мастер для игры не найден.");
    return rows.get(0);
  }

  private static String required(String value, String field) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Поле " + field + " обязательно.");
    return value.trim();
  }

  private static String textOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static Integer positiveOrDefault(Integer value, int fallback) {
    return value == null || value < 1 ? fallback : value;
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static String blankToSystem(String value) {
    return value == null || value.isBlank() ? "system" : value;
  }

  private static String publicTelegramHandle(String username) {
    return username == null || username.isBlank() ? "" : "@" + username.replaceFirst("^@", "");
  }

  public record DashboardResponse(
      String source,
      Instant generatedAt,
      List<MetricDto> metrics,
      List<GameRowDto> upcomingGames,
      List<ActionDto> recentActions
  ) {
  }

  public record ItemsResponse<T>(List<T> content, int page, int size) {
  }

  public record MetricDto(String label, int value, String tone) {
  }

  public record GameRowDto(
      String id,
      String title,
      Instant startsAt,
      String masterPublicId,
      String masterName,
      String gameSystem,
      String status
  ) {
  }

  public record ControlRecordDto(String publicId, String title, String status, Instant updatedAt) {
  }

  public record RatingPlayerRowDto(
      int rank,
      String publicId,
      String displayName,
      String nickname,
      boolean visible,
      int gamesPlayed,
      int totalPoints,
      int inspirationCount,
      BigDecimal averagePointsPerGame,
      Instant lastGameAt,
      Instant lastStatsAt,
      Instant updatedAt
  ) {
  }

  public record ActionDto(String actorPublicId, String action, String entityType, Instant createdAt) {
  }

  public record ProjectDto(String code, String name, String stack, String detectedPath, String status, String launchMode) {
  }

  public record AccountDto(String publicId, String email, List<String> roles, String status) {
  }

  public record StorageDto(StorageAdapterDto media, StorageAdapterDto projectArtifacts, List<String> futureAdapters) {
  }

  public record StorageAdapterDto(String adapter, String root) {
  }

  public record BackupJobDto(String publicId, String status, String checksum, String manifestPath) {
  }

  public record MasterOptionDto(String id, String displayName, String contactUrl) {
  }

  public record AdminGameRequest(
      String title,
      String description,
      String gameSystem,
      String experienceLevel,
      String ageRating,
      String startsAt,
      Integer durationMinutes,
      Integer minPlayers,
      Integer maxPlayers,
      BigDecimal price,
      String currency,
      String masterPublicId,
      String contactUrl,
      String staffNotes
  ) {
  }
}
