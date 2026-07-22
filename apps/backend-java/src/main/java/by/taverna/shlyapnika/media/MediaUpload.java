package by.taverna.shlyapnika.media;

public record MediaUpload(
    byte[] bytes,
    String originalFilename,
    String contentType,
    String namespace,
    String altText
) {
}
