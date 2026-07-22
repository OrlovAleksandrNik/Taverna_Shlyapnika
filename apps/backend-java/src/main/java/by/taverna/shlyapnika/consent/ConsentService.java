package by.taverna.shlyapnika.consent;

import by.taverna.shlyapnika.common.ConsentRequiredException;
import by.taverna.shlyapnika.schedule.domain.ConsentSnapshot;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ConsentService {
  public ConsentSnapshot require(Boolean consentGiven, String consentVersion, String privacyPolicyVersion, String formType) {
    if (!Boolean.TRUE.equals(consentGiven)
        || !ConsentVersions.CONSENT_VERSION.equals(consentVersion)
        || !ConsentVersions.PRIVACY_POLICY_VERSION.equals(privacyPolicyVersion)) {
      throw new ConsentRequiredException();
    }
    return new ConsentSnapshot(consentVersion, privacyPolicyVersion, Instant.now(), formType);
  }
}
