package by.taverna.shlyapnika.bot.telegram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class BotSessionStore {
  private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

  public Session get(long userId) {
    return sessions.getOrDefault(userId, new Session("idle", new GameDraft()));
  }

  public void save(long userId, String state, GameDraft draft) {
    sessions.put(userId, new Session(state, draft));
  }

  public void reset(long userId) {
    sessions.remove(userId);
  }

  public record Session(String state, GameDraft draft) {
  }
}
