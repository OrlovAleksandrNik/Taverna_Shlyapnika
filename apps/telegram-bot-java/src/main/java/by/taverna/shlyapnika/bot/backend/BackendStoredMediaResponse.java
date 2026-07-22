package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackendStoredMediaResponse(StoredMediaDto media) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StoredMediaDto(
      String fileUrl,
      String mediumUrl,
      String thumbnailUrl,
      String mimeType,
      Integer width,
      Integer height,
      String altText
  ) {
  }
}
