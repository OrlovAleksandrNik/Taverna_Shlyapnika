package by.taverna.shlyapnika.bot.telegram;

import by.taverna.shlyapnika.bot.config.BotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramApiClient {
  private static final Logger log = LoggerFactory.getLogger(TelegramApiClient.class);
  private final BotProperties properties;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;

  public TelegramApiClient(BotProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  public JsonNode getUpdates(long offset) throws Exception {
    var uri = apiUri("getUpdates?timeout=" + properties.pollingTimeoutSeconds() + "&offset=" + offset);
    var request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(properties.pollingTimeoutSeconds() + 5L)).GET().build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() >= 400) throw new IllegalStateException("Telegram getUpdates failed with status " + response.statusCode());
    return mapper.readTree(response.body()).path("result");
  }

  public void sendMessage(long chatId, String text) {
    try {
      var payload = "{\"chat_id\":" + chatId + ",\"text\":\"" + escapeJson(text) + "\"}";
      var request = HttpRequest.newBuilder()
          .uri(apiUri("sendMessage"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) log.warn("Telegram sendMessage failed chatId={} status={}", chatId, response.statusCode());
    } catch (Exception error) {
      log.warn("Telegram sendMessage failed chatId={}", chatId, error);
    }
  }

  public void answerCallback(String callbackQueryId) {
    if (callbackQueryId == null || callbackQueryId.isBlank()) return;
    try {
      var uri = apiUri("answerCallbackQuery?callback_query_id=" + encode(callbackQueryId));
      var request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build();
      httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    } catch (Exception error) {
      log.warn("Telegram answerCallbackQuery failed");
    }
  }

  public void deleteWebhook() {
    try {
      var request = HttpRequest.newBuilder()
          .uri(apiUri("deleteWebhook?drop_pending_updates=false"))
          .timeout(Duration.ofSeconds(10))
          .POST(HttpRequest.BodyPublishers.noBody())
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      log.info("Telegram webhook delete requested status={}", response.statusCode());
    } catch (Exception error) {
      log.warn("Telegram deleteWebhook failed", error);
    }
  }

  private URI apiUri(String methodAndQuery) {
    return URI.create("https://api.telegram.org/bot" + properties.token() + "/" + methodAndQuery);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "\\n");
  }
}
