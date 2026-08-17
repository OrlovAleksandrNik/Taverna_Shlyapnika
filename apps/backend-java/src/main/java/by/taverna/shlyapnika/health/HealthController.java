package by.taverna.shlyapnika.health;

import by.taverna.shlyapnika.config.TavernaProperties;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final JdbcTemplate jdbcTemplate;
  private final TavernaProperties properties;

  public HealthController(JdbcTemplate jdbcTemplate, TavernaProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    var database = databaseHealth();
    return Map.of(
        "ok", true,
        "backend", "ok",
        "database", database,
        "bot", botHealth(),
        "checkedAt", Instant.now().toString()
    );
  }

  @GetMapping("/ready")
  public ResponseEntity<Map<String, Object>> ready() {
    var database = databaseHealth();
    var ok = "ok".equals(database);
    var body = Map.of(
        "ok", ok,
        "backend", "ok",
        "database", database,
        "bot", botHealth(),
        "checkedAt", Instant.now().toString()
    );
    return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  private String databaseHealth() {
    try {
      jdbcTemplate.queryForObject("select 1", Integer.class);
      return "ok";
    } catch (Exception error) {
      return "error";
    }
  }

  private Map<String, Object> botHealth() {
    var tokenConfigured = properties.telegram() != null
        && properties.telegram().botToken() != null
        && !properties.telegram().botToken().isBlank();
    if (!tokenConfigured) {
      return Map.of(
          "enabled", false,
          "running", false,
          "mode", "not-configured",
          "lastUpdateAt", ""
      );
    }
    return Map.of(
        "enabled", true,
        "running", true,
        "mode", "external-railway-service",
        "service", "Telegram_Bot",
        "lastUpdateAt", ""
    );
  }
}
