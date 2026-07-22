package by.taverna.shlyapnika.bot.backend;

public record BackendGalleryMediaRequest(
    String fileUrl,
    String thumbnailUrl,
    String mediumUrl,
    Integer width,
    Integer height,
    String mimeType,
    String altText,
    Integer sortOrder
) {
}
