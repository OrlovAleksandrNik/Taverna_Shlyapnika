package by.taverna.shlyapnika.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import by.taverna.shlyapnika.common.ConsentRequiredException;
import org.junit.jupiter.api.Test;

class ConsentServiceTest {
  private final ConsentService service = new ConsentService();

  @Test
  void returnsConsentSnapshotForCurrentVersions() {
    var snapshot = service.require(true, "1.0", "1.0", "game-booking");

    assertThat(snapshot.consentVersion()).isEqualTo("1.0");
    assertThat(snapshot.privacyPolicyVersion()).isEqualTo("1.0");
    assertThat(snapshot.formType()).isEqualTo("game-booking");
    assertThat(snapshot.consentedAt()).isNotNull();
  }

  @Test
  void rejectsMissingConsent() {
    assertThatThrownBy(() -> service.require(false, "1.0", "1.0", "service-request"))
        .isInstanceOf(ConsentRequiredException.class)
        .hasMessage("Заявка не отправлена: необходимо согласие на обработку персональных данных.");
  }

  @Test
  void rejectsOutdatedPolicyVersion() {
    assertThatThrownBy(() -> service.require(true, "1.0", "0.9", "service-request"))
        .isInstanceOf(ConsentRequiredException.class);
  }
}
