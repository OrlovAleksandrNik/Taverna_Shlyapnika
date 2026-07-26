package by.taverna.shlyapnika.access.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MasterAccessRequest(
    @NotBlank @Size(min = 2, max = 80) String displayName,
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(min = 3, max = 80) String telegramUsername,
    Boolean consentGiven,
    String consentVersion,
    String privacyPolicyVersion
) {
}
