package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretEncryptionServiceTest {
  @Test
  void encryptsAndDecryptsSecret() {
    ControlProperties properties = new ControlProperties(
        "http://localhost:4191",
        "http://localhost:4191",
        "session-secret",
        "test-encryption-key",
        false,
        false,
        false,
        false,
        "./media",
        "./backups",
        "",
        "",
        new ControlProperties.Mail("mock", "localhost", 1025, "", "", "test@taverna-control.local", false),
        new ControlProperties.DesktopAgent(java.util.Map.of()));
    SecretEncryptionService service = new SecretEncryptionService(properties);

    String encrypted = service.encrypt("JBSWY3DPEHPK3PXP");

    assertThat(encrypted).isNotEqualTo("JBSWY3DPEHPK3PXP");
    assertThat(service.decrypt(encrypted)).isEqualTo("JBSWY3DPEHPK3PXP");
  }
}
