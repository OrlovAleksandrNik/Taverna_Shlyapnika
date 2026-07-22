package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InternalMasterRequest(
    @NotNull Long telegramUserId,
    String telegramUsername,
    @NotBlank @Size(min = 2, max = 80) String displayName,
    @NotBlank @Size(min = 5, max = 120) String contactUrl
) {
}
