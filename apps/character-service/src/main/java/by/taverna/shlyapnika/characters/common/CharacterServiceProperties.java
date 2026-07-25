package by.taverna.shlyapnika.characters.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "character-service")
public record CharacterServiceProperties(
    String corsAllowedOrigins,
    String apiToken
) {
  public boolean hasApiToken() {
    return apiToken != null && !apiToken.isBlank();
  }
}
