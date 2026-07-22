package by.taverna.shlyapnika.config;

import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "taverna")
public record TavernaProperties(
    @NotBlank String siteBaseUrl,
    @NotBlank String publicUploadsUrl,
    @NotBlank String fileStorageDir,
    @NotBlank String timezone,
    String corsAllowedOrigins,
    @NotBlank String internalApiToken,
    boolean autoPublish,
    Telegram telegram
) {
  public List<String> allowedOrigins() {
    if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
      return List.of("http://localhost:4177", "null");
    }
    return Stream.concat(Arrays.stream(corsAllowedOrigins.split(",")), Stream.of("null"))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .toList();
  }

  public record Telegram(String botToken, String adminIds) {
  }
}
