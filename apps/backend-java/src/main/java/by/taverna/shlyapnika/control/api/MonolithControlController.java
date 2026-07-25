package by.taverna.shlyapnika.control.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonolithControlController {
  private final JdbcTemplate jdbcTemplate;

  public MonolithControlController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static String blankToSystem(String value) {
    return value == null || value.isBlank() ? "system" : value;
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

  public record ActionDto(String actorPublicId, String action, String entityType, Instant createdAt) {
  }
}
