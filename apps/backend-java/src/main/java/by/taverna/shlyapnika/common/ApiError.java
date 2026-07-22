package by.taverna.shlyapnika.common;

import java.time.Instant;

public record ApiError(
    String code,
    String error,
    String message,
    Instant timestamp,
    String path
) {
  public ApiError(String code, String message, Instant timestamp, String path) {
    this(code, message, message, timestamp, path);
  }
}
