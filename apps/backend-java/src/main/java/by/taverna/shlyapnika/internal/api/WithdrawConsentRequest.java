package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.NotBlank;

public record WithdrawConsentRequest(@NotBlank String entityType, @NotBlank String requestId, Boolean anonymize) {
}
