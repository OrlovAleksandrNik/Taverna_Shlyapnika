package by.taverna.shlyapnika.access.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MasterSessionModeRequest(
    @NotBlank @Pattern(regexp = "hatter|master") String mode
) {
}
