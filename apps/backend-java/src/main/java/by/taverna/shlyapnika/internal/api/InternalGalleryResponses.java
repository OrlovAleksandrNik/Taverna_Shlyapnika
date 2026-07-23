package by.taverna.shlyapnika.internal.api;

import java.time.Instant;
import java.util.List;

public final class InternalGalleryResponses {
  private InternalGalleryResponses() {
  }

  public record InternalGalleryPostDto(
      String id,
      String publicId,
      String type,
      String title,
      String description,
      String category,
      Instant eventDate,
      String status,
      int mediaCount,
      List<InternalGalleryMediaDto> media,
      Instant createdAt,
      Instant publishedAt
  ) {
  }

  public record InternalGalleryMediaDto(
      String id,
      String fileUrl,
      String thumbnailUrl,
      String mediumUrl,
      String altText
  ) {
  }

  public record InternalGalleryPostResponse(InternalGalleryPostDto post) {
  }

  public record InternalGalleryListResponse(List<InternalGalleryPostDto> posts) {
  }
}
