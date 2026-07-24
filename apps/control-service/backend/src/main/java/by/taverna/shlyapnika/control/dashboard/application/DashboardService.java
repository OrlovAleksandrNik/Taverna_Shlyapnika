package by.taverna.shlyapnika.control.dashboard.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
  public Map<String, Object> snapshot() {
    return Map.of(
        "source", "mock-control-service",
        "generatedAt", Instant.now(),
        "metrics", List.of(
            Map.of("label", "Ближайшие игры", "value", 7, "tone", "good"),
            Map.of("label", "Новые заявки", "value", 12, "tone", "attention"),
            Map.of("label", "Активные мастера", "value", 5, "tone", "good"),
            Map.of("label", "Ошибки интеграций", "value", 0, "tone", "muted")
        ),
        "upcomingGames", List.of(
            Map.of("title", "Тайна янтарного ключа", "startsAt", "2026-07-26T16:00:00Z", "master", "Станислав", "status", "published"),
            Map.of("title", "Безумное чаепитие", "startsAt", "2026-07-28T18:00:00Z", "master", "Андрей", "status", "draft")
        ),
        "projects", List.of(
            Map.of("code", "voicemod", "status", "mock-ready", "launch", "disabled-without-desktop-agent"),
            Map.of("code", "screenstage", "status", "mock-ready", "launch", "disabled-without-desktop-agent")
        ),
        "recentActions", List.of(
            Map.of("actor", "OWNER", "action", "games.publish", "entity", "Тайна янтарного ключа"),
            Map.of("actor", "CONTENT_MANAGER", "action", "gallery.draft_saved", "entity", "Фото из зала")
        ),
        "backup", Map.of("status", "last-mock-ok", "lastRunAt", "2026-07-24T12:00:00Z")
    );
  }
}
