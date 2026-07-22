package by.taverna.shlyapnika.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taverna.bot")
public record BotProperties(
    String token,
    String mode,
    String backendUrl,
    String internalToken,
    int pollingTimeoutSeconds
) {
  public boolean tokenConfigured() {
    return token != null && !token.isBlank();
  }

  public boolean polling() {
    return mode == null || mode.isBlank() || "polling".equalsIgnoreCase(mode);
  }
}
