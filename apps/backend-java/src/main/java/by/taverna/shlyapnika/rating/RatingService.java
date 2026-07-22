package by.taverna.shlyapnika.rating;

import by.taverna.shlyapnika.rating.api.RatingResponses.RatingPlayerDto;
import by.taverna.shlyapnika.rating.api.RatingResponses.RatingResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RatingService {
  private final JdbcTemplate jdbcTemplate;

  public RatingService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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

  private static Instant toInstant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }
}
