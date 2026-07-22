package by.taverna.shlyapnika.bot.telegram;

import by.taverna.shlyapnika.bot.config.BotProperties;
import by.taverna.shlyapnika.bot.health.BotStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class TelegramPollingService {
  private static final Logger log = LoggerFactory.getLogger(TelegramPollingService.class);
  private final BotProperties properties;
  private final TelegramApiClient telegram;
  private final BotStatus status;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private long offset = 0;

  public TelegramPollingService(BotProperties properties, TelegramApiClient telegram, BotStatus status) {
    this.properties = properties;
    this.telegram = telegram;
    this.status = status;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (!properties.tokenConfigured()) {
      log.warn("Telegram bot token is not configured; Java bot stays disabled");
      return;
    }
    if (!properties.polling()) {
      log.info("Telegram bot mode={} is configured; polling loop is not started", properties.mode());
      return;
    }
    if (!running.compareAndSet(false, true)) return;
    telegram.deleteWebhook();
    status.markRunning(true);
    executor.submit(this::pollLoop);
    log.info("Java Telegram bot polling started");
  }

  @PreDestroy
  public void stop() {
    running.set(false);
    status.markRunning(false);
    executor.shutdownNow();
    log.info("Java Telegram bot polling stopped");
  }

  private void pollLoop() {
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        var updates = telegram.getUpdates(offset);
        if (updates.isArray()) {
          for (var update : updates) {
            handleUpdate(update);
          }
        }
      } catch (Exception error) {
        log.warn("Telegram polling iteration failed", error);
        sleepAfterError();
      }
    }
  }

  private void handleUpdate(JsonNode update) {
    var updateId = update.path("update_id").asLong();
    if (updateId >= offset) offset = updateId + 1;
    status.markUpdate(updateId);
    log.info("Telegram update received updateId={}", updateId);

    var message = update.path("message");
    if (!message.isMissingNode()) {
      handleMessage(message);
      return;
    }

    var callback = update.path("callback_query");
    if (!callback.isMissingNode()) {
      telegram.answerCallback(callback.path("id").asText(""));
    }
  }

  private void handleMessage(JsonNode message) {
    var chatId = message.path("chat").path("id").asLong();
    var text = message.path("text").asText("");
    if ("/start".equals(text.trim())) {
      telegram.sendMessage(chatId, "Добро пожаловать в Таверну Шляпника. Java-бот уже на страже, а игровые сценарии подключаются к новому backend поэтапно.");
      return;
    }
    telegram.sendMessage(chatId, "Я уже слышу вас. Полное меню мастера появится после переноса сценариев игр, рейтинга и галереи в Java.");
  }

  private void sleepAfterError() {
    try {
      Thread.sleep(3_000);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
