package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.databind.JsonNode;

public record BackendBotSessionRequest(String state, JsonNode draft) {
}
