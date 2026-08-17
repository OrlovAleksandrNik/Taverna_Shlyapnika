package by.taverna.shlyapnika.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taverna.bot")
public record BotProperties(
    String token,
    String mode,
    String backendUrl,
    String internalToken,
    int pollingTimeoutSeconds,
    String displayName,
    String shortDescription,
    String cacheChatId,
    long cleanupDelaySeconds,
    String hatterTelegramUserId
) {
  public boolean tokenConfigured() {
    return token != null && !token.isBlank();
  }

  public boolean polling() {
    return mode == null || mode.isBlank() || "polling".equalsIgnoreCase(mode);
  }

  public String safeDisplayName() {
    return displayName == null || displayName.isBlank() ? "Писарь Таверны" : displayName.trim();
  }

  public String safeShortDescription() {
    return shortDescription == null || shortDescription.isBlank()
        ? "Создаёт афиши игр, ведёт галерею и помогает мастерам управлять рейтингом игроков."
        : shortDescription.trim();
  }

  public boolean isHatter(long telegramUserId) {
    if (hatterTelegramUserId == null || hatterTelegramUserId.isBlank()) return false;
    return java.util.Arrays.stream(hatterTelegramUserId.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .anyMatch(value -> value.equals(String.valueOf(telegramUserId)));
  }
}
