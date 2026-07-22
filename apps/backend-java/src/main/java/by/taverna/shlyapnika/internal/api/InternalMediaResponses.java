package by.taverna.shlyapnika.internal.api;

public final class InternalMediaResponses {
  private InternalMediaResponses() {
  }

  public record StoredMediaResponse(StoredMediaDto media) {
  }

  public record StoredMediaDto(
      String storageKey,
      String fileUrl,
      String mediumUrl,
      String thumbnailUrl,
      String mimeType,
      Integer width,
      Integer height,
      long sizeBytes,
      String altText
  ) {
  }
}
