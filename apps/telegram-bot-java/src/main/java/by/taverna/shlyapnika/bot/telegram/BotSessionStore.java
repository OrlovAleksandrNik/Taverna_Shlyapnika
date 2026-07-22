package by.taverna.shlyapnika.bot.telegram;

import by.taverna.shlyapnika.bot.backend.BackendApiClient;
import by.taverna.shlyapnika.bot.backend.BackendBotSessionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class BotSessionStore {
  private final BackendApiClient backend;
  private final ObjectMapper mapper;

  public BotSessionStore(BackendApiClient backend, ObjectMapper mapper) {
    this.backend = backend;
    this.mapper = mapper;
  }

  public Session get(long userId) {
    var session = backend.getBotSession(userId);
    if (session == null) return new Session("idle", new GameDraft());
    return new Session(session.state(), mapper.convertValue(session.draft(), GameDraft.class));
  }

  public void save(long userId, String state, GameDraft draft) {
    backend.saveBotSession(userId, new BackendBotSessionRequest(state, mapper.valueToTree(draft)));
  }

  public void reset(long userId) {
    backend.deleteBotSession(userId);
  }

  public record Session(String state, GameDraft draft) {
  }
}
