package by.taverna.shlyapnika.control.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class DataRecordDtos {
  private DataRecordDtos() {
  }

  public record DataRecordRequest(@NotBlank String title, @NotBlank String payload, Long version) {
  }

  public record DataRecordResponse(
      String publicId,
      String section,
      String title,
      DataRecordStatus status,
      String payload,
      Long version,
      String createdBy,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  public record BulkActionRequest(@NotNull java.util.Set<String> publicIds) {
  }
}
