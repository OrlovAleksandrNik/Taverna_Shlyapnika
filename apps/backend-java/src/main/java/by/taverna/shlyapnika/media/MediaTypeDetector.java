package by.taverna.shlyapnika.media;

import java.util.Optional;

public final class MediaTypeDetector {
  private MediaTypeDetector() {
  }

  public static Optional<DetectedMediaType> detect(byte[] bytes) {
    if (bytes == null || bytes.length < 12) return Optional.empty();
    if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
      return Optional.of(new DetectedMediaType("image/jpeg", "jpg"));
    }
    if ((bytes[0] & 0xff) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4e
        && bytes[3] == 0x47) {
      return Optional.of(new DetectedMediaType("image/png", "png"));
    }
    if (bytes[0] == 0x52
        && bytes[1] == 0x49
        && bytes[2] == 0x46
        && bytes[3] == 0x46
        && bytes[8] == 0x57
        && bytes[9] == 0x45
        && bytes[10] == 0x42
        && bytes[11] == 0x50) {
      return Optional.of(new DetectedMediaType("image/webp", "webp"));
    }
    return Optional.empty();
  }

  public record DetectedMediaType(String mimeType, String extension) {
  }
}
