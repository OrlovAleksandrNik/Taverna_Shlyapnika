package by.taverna.shlyapnika.media;

public record StoredMedia(
    String storageKey,
    String originalUrl,
    String mediumUrl,
    String thumbnailUrl,
    String mimeType,
    Integer width,
    Integer height,
    long sizeBytes,
    String altText
) {
}
