package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackendGalleryPostResponse(PostDto post) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PostDto(String id, String publicId, String type, String title, String category, String status, int mediaCount) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PostsResponse(List<PostDto> posts) {
  }
}
