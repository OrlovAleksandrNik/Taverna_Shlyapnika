package by.taverna.shlyapnika.rating;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.rating.api.RatingResponses.RatingPlayerDto;
import by.taverna.shlyapnika.rating.api.RatingResponses.InternalRatingEventDto;
import by.taverna.shlyapnika.rating.api.RatingResponses.InternalRatingPlayerDto;
import by.taverna.shlyapnika.rating.api.RatingResponses.RatingResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;

  public RatingService(JdbcTemplate jdbcTemplate, AuditService auditService) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
  }

  public RatingResponse listPublicRating(String search, Integer limit, Integer offset) {
    var normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase();
    var safeLimit = Math.min(limit == null ? 100 : limit, 200);
    var safeOffset = Math.max(offset == null ? 0 : offset, 0);

    var topThree = jdbcTemplate.query("""
        select
          row_number() over (
            order by
              "totalPoints" desc,
              case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
              "gamesPlayed" desc,
              lower("displayName") asc,
              "id" asc
          )::int as "rank",
          "id", "displayName", "nickname", "avatarUrl", "gamesPlayed",
          "totalPoints", "inspirationCount",
          round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
          "lastGameAt", "lastStatsAt", "updatedAt"
        from "RatingPlayer"
        where "isVisible" = true
        order by "rank" asc
        limit 3
        """, (rs, rowNum) -> toPlayer(rs));

    var players = jdbcTemplate.query("""
        with ranked as (
          select
            row_number() over (
              order by
                "totalPoints" desc,
                case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
                "gamesPlayed" desc,
                lower("displayName") asc,
                "id" asc
            )::int as "rank",
            "id", "displayName", "nickname", "avatarUrl", "gamesPlayed",
            "totalPoints", "inspirationCount",
            round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
            "lastGameAt", "lastStatsAt", "updatedAt"
          from "RatingPlayer"
          where "isVisible" = true
        )
        select *
        from ranked
        where (?::text is null or lower("displayName") like '%' || ? || '%' or lower(coalesce("nickname", '')) like '%' || ? || '%')
        order by "rank" asc
        limit ? offset ?
        """, (rs, rowNum) -> toPlayer(rs), normalizedSearch, normalizedSearch, normalizedSearch, safeLimit, safeOffset);

    var total = jdbcTemplate.queryForObject("""
        select count(*)::int
        from "RatingPlayer"
        where "isVisible" = true
          and (?::text is null or lower("displayName") like '%' || ? || '%' or lower(coalesce("nickname", '')) like '%' || ? || '%')
        """, Integer.class, normalizedSearch, normalizedSearch, normalizedSearch);

    return new RatingResponse(
        topThree,
        players,
        total == null ? 0 : total,
        "totalPoints DESC, averagePointsPerGame DESC, gamesPlayed DESC, displayName ASC",
        Instant.now()
    );
  }

  public List<InternalRatingPlayerDto> listPlayersForBot(boolean includeHidden) {
    return jdbcTemplate.query("""
        select
          row_number() over (
            order by "totalPoints" desc,
              case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
              "gamesPlayed" desc,
              lower("displayName") asc,
              "id" asc
          )::int as "rank",
          "id", "displayName", "nickname", "avatarUrl", "isVisible", "gamesPlayed",
          "totalPoints", "inspirationCount",
          round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
          "lastGameAt", "lastStatsAt", "updatedAt"
        from "RatingPlayer"
        where (? = true or "isVisible" = true)
        order by "rank" asc
        limit 50
        """, (rs, rowNum) -> toInternalPlayer(rs), includeHidden);
  }

  public List<InternalRatingEventDto> listHistoryForBot(int limit) {
    return jdbcTemplate.query("""
        select e."id", e."playerId", p."displayName", e."type", e."pointsDelta",
          e."inspirationDelta", e."gamesDelta", e."reason", e."createdAt"
        from "RatingEvent" e
        join "RatingPlayer" p on p."id" = e."playerId"
        order by e."createdAt" desc
        limit ?
        """, (rs, rowNum) -> new InternalRatingEventDto(
            rs.getString("id"),
            rs.getString("playerId"),
            rs.getString("displayName"),
            rs.getString("type"),
            rs.getInt("pointsDelta"),
            rs.getInt("inspirationDelta"),
            rs.getInt("gamesDelta"),
            rs.getString("reason"),
            toInstant(rs.getTimestamp("createdAt"))
        ), Math.min(Math.max(limit, 1), 30));
  }

  @Transactional
  public InternalRatingPlayerDto createPlayer(String displayName, String nickname, String avatarUrl, Long telegramUserId, String masterId, String idempotencyKey) {
    var name = displayName.trim();
    var nick = trimToNull(nickname);
    var duplicate = jdbcTemplate.queryForObject("""
        select count(*)::int
        from "RatingPlayer"
        where lower("displayName") = lower(?)
          or (?::text is not null and lower(coalesce("nickname", '')) = lower(?))
        """, Integer.class, name, nick, nick);
    if (duplicate != null && duplicate > 0) throw new IllegalArgumentException("Такой игрок или псевдоним уже есть в рейтинге.");

    var playerId = Ids.newId("rpl");
    jdbcTemplate.update("""
        insert into "RatingPlayer" ("id", "displayName", "nickname", "avatarUrl", "updatedAt")
        values (?, ?, ?, ?, current_timestamp)
        """, playerId, name, nick, trimToNull(avatarUrl));
    var eventId = createRatingEvent(playerId, null, "player_created", 0, 0, 0, "Игрок добавлен в рейтинг через Telegram-бот.", telegramUserId, masterId, null, idempotencyKey);
    auditService.write(telegramUserId == null ? null : String.valueOf(telegramUserId), "rating.player_created", "RatingPlayer", playerId, "{\"eventId\":\"" + eventId + "\"}");
    return findInternalPlayer(playerId);
  }

  @Transactional
  public MutationResult addGameResult(String playerId, int points, String gameTitle, Instant gameDate, String masterName, String reason, Long telegramUserId, String masterId, String idempotencyKey) {
    if (points < 0) throw new IllegalArgumentException("За сыгранную игру нельзя начислить отрицательные очки.");
    var existing = findEventByIdempotency(idempotencyKey);
    if (existing != null) return new MutationResult(existing, null);

    var playedGameId = Ids.newId("rgm");
    jdbcTemplate.update("""
        insert into "RatingPlayedGame" ("id", "title", "gameDate", "masterName", "notes", "createdByTelegramId", "updatedAt")
        values (?, ?, ?, ?, ?, ?, current_timestamp)
        """, playedGameId, gameTitle.trim(), timestamp(gameDate), trimToNull(masterName), trimToNull(reason), telegramUserId);
    var eventId = createRatingEvent(playerId, playedGameId, "game_result", points, 0, 1, trimToNull(reason) == null ? "Сыгранная игра: " + gameTitle.trim() : reason.trim(), telegramUserId, masterId, null, idempotencyKey);
    recalculatePlayerStats(playerId);
    auditService.write(telegramUserId == null ? null : String.valueOf(telegramUserId), "rating.game_result", "RatingPlayer", playerId, "{\"eventId\":\"" + eventId + "\",\"playedGameId\":\"" + playedGameId + "\"}");
    return new MutationResult(eventId, playedGameId);
  }

  @Transactional
  public MutationResult adjustPoints(String playerId, int pointsDelta, String reason, Long telegramUserId, String masterId, String idempotencyKey) {
    if (pointsDelta == 0) throw new IllegalArgumentException("Укажите ненулевое изменение очков.");
    var existing = findEventByIdempotency(idempotencyKey);
    if (existing != null) return new MutationResult(existing, null);
    var eventId = createRatingEvent(playerId, null, pointsDelta < 0 ? "correction" : "points_adjustment", pointsDelta, 0, 0, reason.trim(), telegramUserId, masterId, null, idempotencyKey);
    recalculatePlayerStats(playerId);
    auditService.write(telegramUserId == null ? null : String.valueOf(telegramUserId), "rating.points_adjusted", "RatingPlayer", playerId, "{\"eventId\":\"" + eventId + "\",\"pointsDelta\":" + pointsDelta + "}");
    return new MutationResult(eventId, null);
  }

  @Transactional
  public MutationResult adjustInspiration(String playerId, int inspirationDelta, String reason, Long telegramUserId, String masterId, String idempotencyKey) {
    if (inspirationDelta == 0) throw new IllegalArgumentException("Укажите ненулевое изменение вдохновения.");
    var existing = findEventByIdempotency(idempotencyKey);
    if (existing != null) return new MutationResult(existing, null);
    var eventId = createRatingEvent(playerId, null, inspirationDelta < 0 ? "correction" : "inspiration_adjustment", 0, inspirationDelta, 0, reason.trim(), telegramUserId, masterId, null, idempotencyKey);
    recalculatePlayerStats(playerId);
    auditService.write(telegramUserId == null ? null : String.valueOf(telegramUserId), "rating.inspiration_adjusted", "RatingPlayer", playerId, "{\"eventId\":\"" + eventId + "\",\"inspirationDelta\":" + inspirationDelta + "}");
    return new MutationResult(eventId, null);
  }

  @Transactional
  public MutationResult setPlayerVisibility(String playerId, boolean isVisible, String reason, Long telegramUserId, String masterId, String idempotencyKey) {
    var existing = findEventByIdempotency(idempotencyKey);
    if (existing != null) return new MutationResult(existing, null);
    var updated = jdbcTemplate.update("""
        update "RatingPlayer"
        set "isVisible" = ?, "updatedAt" = current_timestamp
        where "id" = ?
        """, isVisible, playerId);
    if (updated == 0) throw new IllegalArgumentException("Игрок рейтинга не найден.");
    var eventId = createRatingEvent(playerId, null, isVisible ? "player_shown" : "player_hidden", 0, 0, 0, reason.trim(), telegramUserId, masterId, null, idempotencyKey);
    auditService.write(telegramUserId == null ? null : String.valueOf(telegramUserId), isVisible ? "rating.player_shown" : "rating.player_hidden", "RatingPlayer", playerId, "{\"eventId\":\"" + eventId + "\"}");
    return new MutationResult(eventId, null);
  }

  private static RatingPlayerDto toPlayer(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new RatingPlayerDto(
        rs.getInt("rank"),
        rs.getString("id"),
        rs.getString("displayName"),
        rs.getString("nickname"),
        rs.getString("avatarUrl"),
        rs.getInt("gamesPlayed"),
        rs.getInt("totalPoints"),
        rs.getInt("inspirationCount"),
        rs.getBigDecimal("averagePointsPerGame").setScale(2),
        toInstant(rs.getTimestamp("lastGameAt")),
        toInstant(rs.getTimestamp("lastStatsAt") == null ? rs.getTimestamp("updatedAt") : rs.getTimestamp("lastStatsAt"))
    );
  }

  private InternalRatingPlayerDto findInternalPlayer(String playerId) {
    return jdbcTemplate.queryForObject("""
        with ranked as (
          select
            row_number() over (
              order by "totalPoints" desc,
                case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
                "gamesPlayed" desc,
                lower("displayName") asc,
                "id" asc
            )::int as "rank",
            "id", "displayName", "nickname", "avatarUrl", "isVisible", "gamesPlayed",
            "totalPoints", "inspirationCount",
            round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
            "lastGameAt", "lastStatsAt", "updatedAt"
          from "RatingPlayer"
        )
        select * from ranked where "id" = ?
        """, (rs, rowNum) -> toInternalPlayer(rs), playerId);
  }

  private static InternalRatingPlayerDto toInternalPlayer(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new InternalRatingPlayerDto(
        rs.getInt("rank"),
        rs.getString("id"),
        rs.getString("displayName"),
        rs.getString("nickname"),
        rs.getString("avatarUrl"),
        rs.getBoolean("isVisible"),
        rs.getInt("gamesPlayed"),
        rs.getInt("totalPoints"),
        rs.getInt("inspirationCount"),
        rs.getBigDecimal("averagePointsPerGame").setScale(2),
        toInstant(rs.getTimestamp("lastGameAt")),
        toInstant(rs.getTimestamp("lastStatsAt") == null ? rs.getTimestamp("updatedAt") : rs.getTimestamp("lastStatsAt"))
    );
  }

  private String createRatingEvent(
      String playerId,
      String playedGameId,
      String type,
      int pointsDelta,
      int inspirationDelta,
      int gamesDelta,
      String reason,
      Long telegramUserId,
      String masterId,
      String reversalOfEventId,
      String idempotencyKey
  ) {
    ensurePlayerExists(playerId);
    var id = Ids.newId("revt");
    var rows = jdbcTemplate.query("""
        insert into "RatingEvent" (
          "id", "playerId", "playedGameId", "type", "pointsDelta", "inspirationDelta",
          "gamesDelta", "reason", "createdByTelegramId", "createdByMasterId",
          "reversalOfEventId", "idempotencyKey"
        )
        values (?, ?, ?, ?::"RatingEventType", ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict ("idempotencyKey") do nothing
        returning "id"
        """, (rs, rowNum) -> rs.getString("id"),
        id, playerId, playedGameId, type, pointsDelta, inspirationDelta, gamesDelta, reason, telegramUserId, masterId, reversalOfEventId, trimToNull(idempotencyKey));
    return rows.isEmpty() ? findEventByIdempotency(idempotencyKey) : rows.getFirst();
  }

  private void ensurePlayerExists(String playerId) {
    var count = jdbcTemplate.queryForObject("select count(*)::int from \"RatingPlayer\" where \"id\" = ?", Integer.class, playerId);
    if (count == null || count == 0) throw new IllegalArgumentException("Игрок рейтинга не найден.");
  }

  private String findEventByIdempotency(String idempotencyKey) {
    var key = trimToNull(idempotencyKey);
    if (key == null) return null;
    var rows = jdbcTemplate.query("select \"id\" from \"RatingEvent\" where \"idempotencyKey\" = ? limit 1", (rs, rowNum) -> rs.getString("id"), key);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  private void recalculatePlayerStats(String playerId) {
    // RatingPlayer stores cached counters, but RatingEvent is the source of truth.
    jdbcTemplate.update("""
        update "RatingPlayer"
        set
          "totalPoints" = stats."totalPoints",
          "inspirationCount" = stats."inspirationCount",
          "gamesPlayed" = stats."gamesPlayed",
          "lastGameAt" = stats."lastGameAt",
          "lastStatsAt" = current_timestamp,
          "updatedAt" = current_timestamp
        from (
          select
            coalesce(sum(e."pointsDelta"), 0)::int as "totalPoints",
            greatest(coalesce(sum(e."inspirationDelta"), 0), 0)::int as "inspirationCount",
            greatest(coalesce(sum(e."gamesDelta"), 0), 0)::int as "gamesPlayed",
            coalesce(
              max(g."gameDate") filter (where e."gamesDelta" > 0),
              max(e."createdAt") filter (where e."gamesDelta" > 0)
            ) as "lastGameAt"
          from "RatingEvent" e
          left join "RatingPlayedGame" g on g."id" = e."playedGameId"
          where e."playerId" = ?
        ) stats
        where "RatingPlayer"."id" = ?
        """, playerId, playerId);
  }

  private static Instant toInstant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  private static Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record MutationResult(String eventId, String playedGameId) {
  }
}
