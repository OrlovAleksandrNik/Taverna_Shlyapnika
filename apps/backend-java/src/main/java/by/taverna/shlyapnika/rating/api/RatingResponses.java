package by.taverna.shlyapnika.rating.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class RatingResponses {
  private RatingResponses() {
  }

  public record RatingPlayerDto(
      int rank,
      String id,
      String displayName,
      String nickname,
      String avatarUrl,
      int gamesPlayed,
      int totalPoints,
      int inspirationCount,
      BigDecimal averagePointsPerGame,
      Instant lastGameAt,
      Instant lastStatsAt
  ) {
  }

  public record InternalRatingPlayerDto(
      int rank,
      String id,
      String displayName,
      String nickname,
      String avatarUrl,
      boolean isVisible,
      int gamesPlayed,
      int totalPoints,
      int inspirationCount,
      BigDecimal averagePointsPerGame,
      Instant lastGameAt,
      Instant lastStatsAt
  ) {
  }

  public record InternalRatingEventDto(
      String id,
      String playerId,
      String displayName,
      String type,
      int pointsDelta,
      int inspirationDelta,
      int gamesDelta,
      String reason,
      Instant createdAt
  ) {
  }

  public record RatingResponse(
      List<RatingPlayerDto> topThree,
      List<RatingPlayerDto> players,
      int total,
      String sort,
      Instant updatedAt
  ) {
  }
}
