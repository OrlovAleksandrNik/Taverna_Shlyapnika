package by.taverna.shlyapnika.access.api;

import jakarta.validation.constraints.Size;

public record MasterLoginRequest(
    @Size(max = 160) String email,
    @Size(max = 80) String telegramUsername
) {
}
