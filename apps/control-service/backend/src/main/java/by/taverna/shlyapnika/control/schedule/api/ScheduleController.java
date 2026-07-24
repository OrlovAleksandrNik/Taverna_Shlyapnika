package by.taverna.shlyapnika.control.schedule.api;

import by.taverna.shlyapnika.control.games.api.GameDtos.GameResponse;
import by.taverna.shlyapnika.control.games.api.GamesController;
import by.taverna.shlyapnika.control.games.infrastructure.ControlGameRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/schedule")
public class ScheduleController {
  private final ControlGameRepository games;

  public ScheduleController(ControlGameRepository games) {
    this.games = games;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('schedule.read')")
  List<GameResponse> calendar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    return games.findByDeletedAtIsNullAndStartsAtBetweenOrderByStartsAtAsc(from, to).stream().map(GamesController::toResponse).toList();
  }
}
