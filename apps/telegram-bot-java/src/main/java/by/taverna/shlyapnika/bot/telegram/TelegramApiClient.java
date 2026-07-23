package by.taverna.shlyapnika.bot.telegram;

import by.taverna.shlyapnika.bot.config.BotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramApiClient {
  private static final Logger log = LoggerFactory.getLogger(TelegramApiClient.class);
  private final BotProperties properties;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;
  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

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

  public Long sendMessage(long chatId, String text) {
    return sendMessage(chatId, text, null);
  }

  public Long sendMessage(long chatId, String text, Object replyMarkup) {
    return sendMessageInternal(chatId, text, replyMarkup, true);
  }

  private Long sendMessageInternal(long chatId, String text, Object replyMarkup, boolean managedLifecycle) {
    try {
      var payload = replyMarkup == null
          ? Map.of("chat_id", chatId, "text", text)
          : Map.of("chat_id", chatId, "text", text, "reply_markup", replyMarkup);
      var request = HttpRequest.newBuilder()
          .uri(apiUri("sendMessage"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
        log.warn("Telegram sendMessage failed chatId={} status={}", chatId, response.statusCode());
        return null;
      }
      var messageId = mapper.readTree(response.body()).path("result").path("message_id").asLong(0);
      log.info("Telegram sendMessage delivered chatId={} messageId={}", chatId, messageId);
      if (managedLifecycle && messageId > 0) {
        mirrorToCache(chatId, text);
        scheduleCleanup(chatId, messageId);
      }
      return messageId == 0 ? null : messageId;
    } catch (Exception error) {
      log.warn("Telegram sendMessage failed chatId={}", chatId, error);
      return null;
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

  public TelegramFile downloadFile(String fileId) {
    try {
      var fileInfoRequest = HttpRequest.newBuilder()
          .uri(apiUri("getFile?file_id=" + encode(fileId)))
          .timeout(Duration.ofSeconds(10))
          .GET()
          .build();
      var fileInfoResponse = httpClient.send(fileInfoRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (fileInfoResponse.statusCode() >= 400) throw new IllegalStateException("Telegram getFile failed with status " + fileInfoResponse.statusCode());
      var filePath = mapper.readTree(fileInfoResponse.body()).path("result").path("file_path").asText("");
      if (filePath.isBlank()) throw new IllegalStateException("Telegram did not return file_path");

      var downloadRequest = HttpRequest.newBuilder()
          .uri(URI.create("https://api.telegram.org/file/bot" + properties.token() + "/" + filePath))
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();
      var downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
      if (downloadResponse.statusCode() >= 400) throw new IllegalStateException("Telegram file download failed with status " + downloadResponse.statusCode());
      return new TelegramFile(downloadResponse.body(), filename(filePath), contentType(filePath));
    } catch (Exception error) {
      log.warn("Telegram file download failed fileId={}", fileId, error);
      throw new IllegalStateException("Не удалось скачать файл из Telegram. Попробуйте отправить изображение ещё раз.");
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

  public void setMyName(String name) {
    if (name == null || name.isBlank()) return;
    try {
      var request = HttpRequest.newBuilder()
          .uri(apiUri("setMyName"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("name", name.trim())), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) log.warn("Telegram setMyName failed status={}", response.statusCode());
    } catch (Exception error) {
      log.warn("Telegram setMyName failed", error);
    }
  }

  public void setMyShortDescription(String description) {
    if (description == null || description.isBlank()) return;
    try {
      var request = HttpRequest.newBuilder()
          .uri(apiUri("setMyShortDescription"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("short_description", description.trim())), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) log.warn("Telegram setMyShortDescription failed status={}", response.statusCode());
    } catch (Exception error) {
      log.warn("Telegram setMyShortDescription failed", error);
    }
  }

  public void setMyCommands(List<Map<String, String>> commands) {
    if (commands == null || commands.isEmpty()) return;
    try {
      var payload = Map.of("commands", commands, "language_code", "ru");
      var request = HttpRequest.newBuilder()
          .uri(apiUri("setMyCommands"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) log.warn("Telegram setMyCommands failed status={}", response.statusCode());
    } catch (Exception error) {
      log.warn("Telegram setMyCommands failed", error);
    }
  }

  public void setCommandsMenuButton() {
    try {
      var payload = Map.of("menu_button", Map.of("type", "commands"));
      var request = HttpRequest.newBuilder()
          .uri(apiUri("setChatMenuButton"))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) log.warn("Telegram setChatMenuButton failed status={}", response.statusCode());
    } catch (Exception error) {
      log.warn("Telegram setChatMenuButton failed", error);
    }
  }

  public void deleteMessage(long chatId, long messageId) {
    try {
      var request = HttpRequest.newBuilder()
          .uri(apiUri("deleteMessage?chat_id=" + chatId + "&message_id=" + messageId))
          .timeout(Duration.ofSeconds(10))
          .POST(HttpRequest.BodyPublishers.noBody())
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400) log.debug("Telegram deleteMessage skipped chatId={} messageId={} status={}", chatId, messageId, response.statusCode());
    } catch (Exception error) {
      log.debug("Telegram deleteMessage failed chatId={} messageId={}", chatId, messageId);
    }
  }

  @PreDestroy
  public void shutdown() {
    cleanupExecutor.shutdownNow();
  }

  private URI apiUri(String methodAndQuery) {
    return URI.create("https://api.telegram.org/bot" + properties.token() + "/" + methodAndQuery);
  }

  private void mirrorToCache(long sourceChatId, String text) {
    var cacheChatId = parseChatId(properties.cacheChatId());
    if (cacheChatId == null || cacheChatId == sourceChatId) return;
    var archived = "Кэш сообщений бота\nЧат: " + sourceChatId + "\n\n" + truncate(text, 3400);
    sendMessageInternal(cacheChatId, archived, null, false);
  }

  private void scheduleCleanup(long chatId, long messageId) {
    var delay = properties.cleanupDelaySeconds();
    if (delay <= 0) return;
    cleanupExecutor.schedule(() -> deleteMessage(chatId, messageId), delay, TimeUnit.SECONDS);
  }

  private Long parseChatId(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException error) {
      log.warn("Invalid BOT_CACHE_CHAT_ID is ignored");
      return null;
    }
  }

  private String truncate(String value, int limit) {
    if (value == null) return "";
    return value.length() <= limit ? value : value.substring(0, limit - 1) + "…";
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String filename(String filePath) {
    var index = filePath.lastIndexOf('/');
    return index < 0 ? filePath : filePath.substring(index + 1);
  }

  private String contentType(String filePath) {
    var lower = filePath.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".webp")) return "image/webp";
    return "application/octet-stream";
  }

  public record TelegramFile(byte[] bytes, String filename, String contentType) {
  }

}
