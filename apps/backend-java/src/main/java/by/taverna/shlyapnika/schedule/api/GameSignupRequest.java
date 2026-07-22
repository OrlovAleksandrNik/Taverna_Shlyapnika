package by.taverna.shlyapnika.schedule.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameSignupRequest(
    @NotBlank String gameId,
    @NotBlank @Size(min = 2, max = 80) String playerName,
    @NotBlank @Size(min = 3, max = 120) String contact,
    @Min(1) @Max(20) Integer seats,
    @Size(max = 1000) String comment,
    Boolean consentGiven,
    String consentVersion,
    String privacyPolicyVersion
) {
}
