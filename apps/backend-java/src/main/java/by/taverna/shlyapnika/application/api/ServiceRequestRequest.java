package by.taverna.shlyapnika.application.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequestRequest(
    @NotBlank @Size(min = 2, max = 80) String name,
    @NotBlank @Size(min = 3, max = 120) String contact,
    @NotBlank @Size(min = 2, max = 120) String service,
    @Size(max = 80) String serviceType,
    @Size(max = 80) String desiredDate,
    @Min(1) @Max(200) Integer participants,
    @Size(max = 80) String city,
    @Size(max = 1500) String comment,
    Boolean consentGiven,
    String consentVersion,
    String privacyPolicyVersion
) {
}
