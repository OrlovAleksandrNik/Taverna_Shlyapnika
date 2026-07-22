package by.taverna.shlyapnika.bot.backend;

import by.taverna.shlyapnika.bot.config.BotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BackendApiClient {
  private static final Logger log = LoggerFactory.getLogger(BackendApiClient.class);

  private final BotProperties properties;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;

  public BackendApiClient(BotProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  public BackendMasterResponse findMasterByTelegram(long telegramUserId) {
    try {
      var request = baseRequest("/api/internal/masters/by-telegram/" + telegramUserId).GET().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() == 404) return null;
      ensureSuccess(response, "find master");
      return mapper.readValue(response.body(), BackendMasterResponse.class);
    } catch (Exception error) {
      log.warn("Backend master lookup failed telegramUserId={}", telegramUserId, error);
      throw new IllegalStateException("Не удалось проверить профиль мастера. Попробуйте немного позже.");
    }
  }

  public BackendMasterResponse upsertMaster(BackendMasterRequest body) {
    try {
      var request = baseRequest("/api/internal/masters")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      ensureSuccess(response, "upsert master");
      return mapper.readValue(response.body(), BackendMasterResponse.class);
    } catch (Exception error) {
      log.warn("Backend master upsert failed telegramUserId={}", body.telegramUserId(), error);
      throw new IllegalStateException("Не удалось сохранить профиль мастера. Попробуйте немного позже.");
    }
  }

  public BackendGameResponse createGame(BackendGameRequest body) {
    try {
      var request = baseRequest("/api/internal/games")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      ensureSuccess(response, "create game");
      return mapper.readValue(response.body(), BackendGameResponse.class);
    } catch (Exception error) {
      log.warn("Backend game creation failed masterId={}", body.masterId(), error);
      throw new IllegalStateException("Не удалось опубликовать игру. Проверьте данные или попробуйте немного позже.");
    }
  }

  public BackendGamesResponse listMasterGames(String masterId, String scope) {
    try {
      var request = baseRequest("/api/internal/masters/" + masterId + "/games?scope=" + encode(scope == null ? "all" : scope)).GET().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      ensureSuccess(response, "list master games");
      return mapper.readValue(response.body(), BackendGamesResponse.class);
    } catch (Exception error) {
      log.warn("Backend master games lookup failed masterId={}", masterId, error);
      throw new IllegalStateException("Не удалось загрузить список игр. Попробуйте немного позже.");
    }
  }

  public BackendBotSessionResponse getBotSession(long telegramUserId) {
    try {
      var request = baseRequest("/api/internal/bot-sessions/" + telegramUserId).GET().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() == 404) return null;
      ensureSuccess(response, "get bot session");
      return mapper.readValue(response.body(), BackendBotSessionResponse.class);
    } catch (Exception error) {
      log.warn("Backend bot session lookup failed telegramUserId={}", telegramUserId, error);
      throw new IllegalStateException("Не удалось загрузить черновик диалога. Попробуйте немного позже.");
    }
  }

  public BackendBotSessionResponse saveBotSession(long telegramUserId, BackendBotSessionRequest body) {
    try {
      var request = baseRequest("/api/internal/bot-sessions/" + telegramUserId)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      ensureSuccess(response, "save bot session");
      return mapper.readValue(response.body(), BackendBotSessionResponse.class);
    } catch (Exception error) {
      log.warn("Backend bot session save failed telegramUserId={}", telegramUserId, error);
      throw new IllegalStateException("Не удалось сохранить черновик диалога. Попробуйте немного позже.");
    }
  }

  public void deleteBotSession(long telegramUserId) {
    try {
      var request = baseRequest("/api/internal/bot-sessions/" + telegramUserId).DELETE().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 400 && response.statusCode() != 404) {
        throw new IllegalStateException("Backend delete bot session failed with status " + response.statusCode());
      }
    } catch (Exception error) {
      log.warn("Backend bot session delete failed telegramUserId={}", telegramUserId, error);
      throw new IllegalStateException("Не удалось сбросить черновик диалога. Попробуйте немного позже.");
    }
  }

  public BackendGameResponse setMasterGameStatus(String masterId, String gameId, String status) {
    try {
      var body = java.util.Map.of("status", status);
      var request = baseRequest("/api/internal/masters/" + masterId + "/games/" + gameId + "/status")
          .header("Content-Type", "application/json")
          .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      ensureSuccess(response, "set master game status");
      return mapper.readValue(response.body(), BackendGameResponse.class);
    } catch (Exception error) {
      log.warn("Backend master game status update failed masterId={} gameId={}", masterId, gameId, error);
      throw new IllegalStateException("Не удалось изменить статус игры. Попробуйте немного позже.");
    }
  }

  private HttpRequest.Builder baseRequest(String path) {
    return HttpRequest.newBuilder()
        .uri(URI.create(normalizeBaseUrl(properties.backendUrl()) + path))
        .timeout(Duration.ofSeconds(15))
        .header("x-internal-token", properties.internalToken());
  }

  private void ensureSuccess(HttpResponse<String> response, String action) {
    if (response.statusCode() < 400) return;
    var message = backendMessage(response.body());
    throw new IllegalStateException("Backend " + action + " failed with status " + response.statusCode() + ": " + message);
  }

  private String backendMessage(String body) {
    try {
      JsonNode json = mapper.readTree(body);
      return json.path("message").asText(json.path("error").asText("unknown error"));
    } catch (Exception ignored) {
      return "unknown error";
    }
  }

  private String normalizeBaseUrl(String value) {
    var url = value == null || value.isBlank() ? "http://localhost:8080" : value.trim();
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
