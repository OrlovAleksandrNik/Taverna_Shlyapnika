package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MasterAccessDecisionRequest(
    @NotNull Long adminTelegramId,
    @Size(max = 500) String comment
) {
}
