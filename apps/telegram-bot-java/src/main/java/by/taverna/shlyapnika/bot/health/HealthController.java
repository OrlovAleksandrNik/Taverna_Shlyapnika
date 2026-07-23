package by.taverna.shlyapnika.bot.health;

import by.taverna.shlyapnika.bot.config.BotProperties;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final BotProperties properties;
  private final BotStatus status;

  public HealthController(BotProperties properties, BotStatus status) {
    this.properties = properties;
    this.status = status;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of(
        "ok", true,
        "bot", Map.of(
            "enabled", properties.tokenConfigured(),
            "running", status.running(),
            "mode", properties.mode() == null ? "polling" : properties.mode(),
            "lastUpdateId", status.lastUpdateId(),
            "lastUpdateAt", status.lastUpdateAt() == null ? "" : status.lastUpdateAt().toString(),
            "lastPollingErrorAt", status.lastPollingErrorAt() == null ? "" : status.lastPollingErrorAt().toString(),
            "lastPollingError", status.lastPollingError()
        ),
        "checkedAt", Instant.now().toString()
    );
  }

  @GetMapping("/ready")
  public Map<String, Object> ready() {
    return Map.of(
        "ok", properties.tokenConfigured() && status.running(),
        "tokenConfigured", properties.tokenConfigured(),
        "running", status.running(),
        "checkedAt", Instant.now().toString()
    );
  }
}
