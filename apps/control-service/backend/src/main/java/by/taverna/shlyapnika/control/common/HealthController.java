package by.taverna.shlyapnika.control.common;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  @GetMapping("/health")
  Map<String, Object> health() {
    return Map.of("status", "UP", "service", "control-service-backend", "checkedAt", Instant.now());
  }

  @GetMapping("/ready")
  Map<String, Object> ready() {
    return Map.of("status", "READY", "service", "control-service-backend", "checkedAt", Instant.now());
  }
}
