package by.taverna.shlyapnika.internal.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InternalGalleryPostRequest(
    @NotBlank String type,
    @NotBlank @Size(min = 3, max = 120) String title,
    @Size(max = 700) String description,
    @Size(max = 8000) String storyContent,
    @Size(max = 20000) String storyHtml,
    @NotBlank String category,
    String eventDate,
    @NotBlank String status,
    @Valid List<InternalGalleryMediaRequest> media
) {
}
