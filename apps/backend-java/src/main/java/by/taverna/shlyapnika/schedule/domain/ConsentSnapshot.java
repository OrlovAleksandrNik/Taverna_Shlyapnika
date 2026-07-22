package by.taverna.shlyapnika.schedule.domain;

import java.time.Instant;

public record ConsentSnapshot(
    String consentVersion,
    String privacyPolicyVersion,
    Instant consentedAt,
    String formType
) {
}
