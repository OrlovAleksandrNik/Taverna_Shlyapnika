package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InternalRatingRequests {
  private InternalRatingRequests() {
  }

  public record CreatePlayerRequest(
      @NotBlank @Size(min = 2, max = 100) String displayName,
      @Size(max = 80) String nickname,
      String avatarUrl,
      Long createdByTelegramId,
      String idempotencyKey
  ) {
  }

  public record GameResultRequest(
      @NotBlank String playerId,
      @NotNull Integer points,
      @NotBlank @Size(min = 2, max = 160) String gameTitle,
      String gameDate,
      @Size(max = 100) String masterName,
      @Size(max = 500) String reason,
      Long createdByTelegramId,
      String idempotencyKey
  ) {
  }

  public record PointsAdjustmentRequest(
      @NotBlank String playerId,
      @NotNull Integer pointsDelta,
      @NotBlank @Size(min = 2, max = 500) String reason,
      Long createdByTelegramId,
      String idempotencyKey
  ) {
  }

  public record InspirationAdjustmentRequest(
      @NotBlank String playerId,
      @NotNull Integer inspirationDelta,
      @NotBlank @Size(min = 2, max = 500) String reason,
      Long createdByTelegramId,
      String idempotencyKey
  ) {
  }

  public record VisibilityRequest(
      @NotBlank String playerId,
      boolean isVisible,
      @NotBlank @Size(min = 2, max = 500) String reason,
      Long createdByTelegramId,
      String idempotencyKey
  ) {
  }
}
