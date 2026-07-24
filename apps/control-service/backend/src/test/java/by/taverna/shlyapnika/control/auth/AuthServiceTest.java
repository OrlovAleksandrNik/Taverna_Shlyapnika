package by.taverna.shlyapnika.control.auth;

import by.taverna.shlyapnika.control.auth.application.AuthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {
  @Test
  void sha256DoesNotReturnPlainToken() {
    String token = "one-time-invitation-token";

    assertThat(AuthService.sha256(token))
        .isNotEqualTo(token)
        .hasSize(64);
  }
}
