package by.taverna.shlyapnika.characters.common;

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

  public HealthController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/health")
  Map<String, Object> health() {
    return Map.of(
        "ok", true,
        "service", "character-service",
        "database", databaseHealth(),
        "checkedAt", Instant.now().toString()
    );
  }

  @GetMapping("/ready")
  ResponseEntity<Map<String, Object>> ready() {
    var database = databaseHealth();
    var ok = "ok".equals(database);
    return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of(
            "ok", ok,
            "service", "character-service",
            "database", database,
            "checkedAt", Instant.now().toString()
        ));
  }

  private String databaseHealth() {
    try {
      jdbcTemplate.queryForObject("select 1", Integer.class);
      return "ok";
    } catch (Exception error) {
      return "error";
    }
  }
}
