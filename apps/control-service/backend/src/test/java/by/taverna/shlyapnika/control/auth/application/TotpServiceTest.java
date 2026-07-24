package by.taverna.shlyapnika.control.auth.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {
  @Test
  void generatedSecretIsBase32AndOtpauthUrlDoesNotExposePassword() {
    TotpService service = new TotpService();

    String secret = service.generateSecret();
    String url = service.otpauthUrl("Taverna Control", "owner@example.test", secret);

    assertThat(secret).matches("[A-Z2-7]+");
    assertThat(url).startsWith("otpauth://totp/Taverna%20Control");
    assertThat(url).contains("secret=" + secret);
    assertThat(url).doesNotContain("password");
  }
}
