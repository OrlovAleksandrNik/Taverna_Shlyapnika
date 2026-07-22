package by.taverna.shlyapnika.gallery.api;

import java.time.Instant;
import java.util.List;

public final class GalleryResponses {
  private GalleryResponses() {
  }

  public record CategoryDto(String value) {
  }

  public record CategoriesResponse(List<CategoryDto> categories) {
  }

  public record MasterDto(String id, String name) {
  }

  public record MediaDto(
      String id,
      String fileUrl,
      String thumbnailUrl,
      String mediumUrl,
      Integer width,
      Integer height,
      String mimeType,
      String altText
  ) {
  }

  public record GalleryPostDto(
      String id,
      String publicId,
      String type,
      String title,
      String description,
      String storyHtml,
      String category,
      Instant eventDate,
      MasterDto master,
      List<MediaDto> media,
      Instant createdAt,
      Instant publishedAt
  ) {
  }

  public record GalleryListResponse(List<GalleryPostDto> posts) {
  }

  public record GalleryPostResponse(GalleryPostDto post) {
  }
}
