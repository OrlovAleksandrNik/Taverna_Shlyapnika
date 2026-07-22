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

  public record RatingResponse(
      List<RatingPlayerDto> topThree,
      List<RatingPlayerDto> players,
      int total,
      String sort,
      Instant updatedAt
  ) {
  }
}
