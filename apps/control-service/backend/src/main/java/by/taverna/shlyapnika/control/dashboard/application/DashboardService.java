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
            Map.of("label", "Upcoming games", "value", 7, "tone", "good"),
            Map.of("label", "New applications", "value", 12, "tone", "attention"),
            Map.of("label", "Active masters", "value", 5, "tone", "good"),
            Map.of("label", "Integration errors", "value", 0, "tone", "muted")
        ),
        "upcomingGames", List.of(
            Map.of("title", "Amber Key Mystery", "startsAt", "2026-07-26T16:00:00Z", "master", "Stanislav", "status", "published"),
            Map.of("title", "Mad Tea Game", "startsAt", "2026-07-28T18:00:00Z", "master", "Andrey", "status", "draft")
        ),
        "projects", List.of(
            Map.of("code", "voicemod", "status", "mock-ready", "launch", "disabled-without-desktop-agent"),
            Map.of("code", "screenstage", "status", "mock-ready", "launch", "disabled-without-desktop-agent")
        ),
        "recentActions", List.of(
            Map.of("actor", "OWNER", "action", "games.publish", "entity", "Amber Key Mystery"),
            Map.of("actor", "CONTENT_MANAGER", "action", "gallery.draft_saved", "entity", "Hall photo")
        ),
        "backup", Map.of("status", "last-mock-ok", "lastRunAt", "2026-07-24T12:00:00Z")
    );
  }
}
