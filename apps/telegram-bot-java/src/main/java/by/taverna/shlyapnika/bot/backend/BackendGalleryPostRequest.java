package by.taverna.shlyapnika.bot.backend;

import java.util.List;

public record BackendGalleryPostRequest(
    String type,
    String title,
    String description,
    String storyContent,
    String category,
    String eventDate,
    String status,
    List<BackendGalleryMediaRequest> media
) {
}
