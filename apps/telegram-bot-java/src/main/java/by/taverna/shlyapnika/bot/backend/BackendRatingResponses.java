package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class BackendRatingResponses {
  private BackendRatingResponses() {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlayerDto(
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EventDto(String id, String playerId, String displayName, String type, int pointsDelta, int inspirationDelta, int gamesDelta, String reason, Instant createdAt) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlayersResponse(List<PlayerDto> players) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record HistoryResponse(List<EventDto> events) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlayerResponse(PlayerDto player) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MutationResponse(String eventId, String playedGameId) {
  }
}
