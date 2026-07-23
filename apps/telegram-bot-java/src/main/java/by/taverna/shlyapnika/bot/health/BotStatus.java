package by.taverna.shlyapnika.bot.health;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class BotStatus {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicLong lastUpdateId = new AtomicLong(0);
  private final AtomicReference<Instant> lastUpdateAt = new AtomicReference<>();
  private final AtomicReference<Instant> lastPollingErrorAt = new AtomicReference<>();
  private final AtomicReference<String> lastPollingError = new AtomicReference<>("");

  public boolean running() {
    return running.get();
  }

  public long lastUpdateId() {
    return lastUpdateId.get();
  }

  public Instant lastUpdateAt() {
    return lastUpdateAt.get();
  }

  public Instant lastPollingErrorAt() {
    return lastPollingErrorAt.get();
  }

  public String lastPollingError() {
    return lastPollingError.get();
  }

  public void markRunning(boolean value) {
    running.set(value);
  }

  public void markUpdate(long updateId) {
    lastUpdateId.set(updateId);
    lastUpdateAt.set(Instant.now());
    clearPollingError();
  }

  public void markPollingError(Exception error) {
    lastPollingErrorAt.set(Instant.now());
    lastPollingError.set(error == null ? "unknown error" : error.getClass().getSimpleName() + ": " + error.getMessage());
  }

  public void clearPollingError() {
    lastPollingError.set("");
  }
}
