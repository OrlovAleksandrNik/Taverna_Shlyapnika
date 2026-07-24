package by.taverna.shlyapnika.control.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptLimiter {
  private static final int MAX_FAILURES = 5;
  private static final Duration WINDOW = Duration.ofMinutes(15);
  private static final Duration LOCK = Duration.ofMinutes(10);
  private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

  public void assertAllowed(String email, String ipAddress) {
    AttemptState state = attempts.get(key(email, ipAddress));
    if (state != null && state.lockedUntil != null && state.lockedUntil.isAfter(Instant.now())) {
      throw new IllegalArgumentException("Invalid credentials.");
    }
  }

  public void recordSuccess(String email, String ipAddress) {
    attempts.remove(key(email, ipAddress));
  }

  public void recordFailure(String email, String ipAddress) {
    attempts.compute(key(email, ipAddress), (ignored, current) -> {
      Instant now = Instant.now();
      if (current == null || current.firstFailure.plus(WINDOW).isBefore(now)) {
        return new AttemptState(1, now, null);
      }
      int failures = current.failures + 1;
      Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK) : current.lockedUntil;
      return new AttemptState(failures, current.firstFailure, lockedUntil);
    });
  }

  private static String key(String email, String ipAddress) {
    return (email == null ? "" : email.toLowerCase()) + "|" + (ipAddress == null ? "" : ipAddress);
  }

  private record AttemptState(int failures, Instant firstFailure, Instant lockedUntil) {
  }
}
