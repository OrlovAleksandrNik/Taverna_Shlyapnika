package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.databind.JsonNode;

public record BackendBotSessionResponse(long telegramUserId, String state, JsonNode draft) {
}
