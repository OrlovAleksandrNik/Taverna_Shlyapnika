package by.taverna.shlyapnika.notification;

import by.taverna.shlyapnika.config.TavernaProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramNotificationService {
  private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
  private final TavernaProperties properties;
  private final HttpClient httpClient;

  public TelegramNotificationService(TavernaProperties properties) {
    this.properties = properties;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  public void notifyAdmins(String text) {
    for (var chatId : adminIds()) {
      notifyTelegram(chatId, text);
    }
  }

  public void notifyTelegram(Long chatId, String text) {
    if (chatId == null || !tokenConfigured()) return;
    try {
      var request = HttpRequest.newBuilder()
          .uri(URI.create("https://api.telegram.org/bot" + properties.telegram().botToken() + "/sendMessage"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body(chatId, text), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) {
        log.warn("telegram notification failed chatId={} status={}", chatId, response.statusCode());
      }
    } catch (Exception error) {
      log.warn("telegram notification failed chatId={}", chatId, error);
    }
  }

  private List<Long> adminIds() {
    if (properties.telegram() == null || properties.telegram().adminIds() == null || properties.telegram().adminIds().isBlank()) {
      return List.of();
    }
    return Arrays.stream(properties.telegram().adminIds().split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(this::parseChatId)
        .filter(Objects::nonNull)
        .toList();
  }

  private Long parseChatId(String value) {
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException error) {
      log.warn("invalid admin telegram id is ignored");
      return null;
    }
  }

  private boolean tokenConfigured() {
    return properties.telegram() != null
        && properties.telegram().botToken() != null
        && !properties.telegram().botToken().isBlank();
  }

  private String body(Long chatId, String text) {
    return "{\"chat_id\":" + chatId + ",\"text\":\"" + escapeJson(text) + "\"}";
  }

  private String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "\\n");
  }
}
