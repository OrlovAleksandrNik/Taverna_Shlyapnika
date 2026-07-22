package by.taverna.shlyapnika.bot.backend;

public final class BackendRatingRequests {
  private BackendRatingRequests() {
  }

  public record CreatePlayer(String displayName, String nickname, String avatarUrl, Long createdByTelegramId, String idempotencyKey) {
  }

  public record GameResult(String playerId, Integer points, String gameTitle, String gameDate, String masterName, String reason, Long createdByTelegramId, String idempotencyKey) {
  }

  public record PointsAdjustment(String playerId, Integer pointsDelta, String reason, Long createdByTelegramId, String idempotencyKey) {
  }

  public record InspirationAdjustment(String playerId, Integer inspirationDelta, String reason, Long createdByTelegramId, String idempotencyKey) {
  }

  public record Visibility(String playerId, boolean isVisible, String reason, Long createdByTelegramId, String idempotencyKey) {
  }
}
