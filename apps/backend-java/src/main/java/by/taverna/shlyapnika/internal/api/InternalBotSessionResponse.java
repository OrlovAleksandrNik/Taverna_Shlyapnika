package by.taverna.shlyapnika.internal.api;

import com.fasterxml.jackson.databind.JsonNode;

public record InternalBotSessionResponse(
    Long telegramUserId,
    String state,
    JsonNode draft
) {
}
