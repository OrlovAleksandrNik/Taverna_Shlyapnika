package by.taverna.shlyapnika.control.auth.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {
  @Test
  void locksAfterRepeatedFailures() {
    LoginAttemptLimiter limiter = new LoginAttemptLimiter();

    for (int attempt = 0; attempt < 5; attempt++) {
      limiter.recordFailure("owner@example.test", "127.0.0.1");
    }

    assertThatThrownBy(() -> limiter.assertAllowed("owner@example.test", "127.0.0.1"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
