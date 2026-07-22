package by.taverna.shlyapnika.internal.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record InternalBotSessionRequest(
    @NotBlank String state,
    JsonNode draft
) {
}
