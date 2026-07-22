package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalGalleryMediaRequest(
    @NotBlank String fileUrl,
    @NotBlank String thumbnailUrl,
    @NotBlank String mediumUrl,
    Integer width,
    Integer height,
    @NotBlank String mimeType,
    @Size(max = 200) String altText,
    Integer sortOrder
) {
}
