package by.taverna.shlyapnika.schedule.api;

import by.taverna.shlyapnika.schedule.ScheduleService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduleController {
  private final ScheduleService schedule;

  public ScheduleController(ScheduleService schedule) {
    this.schedule = schedule;
  }

  @GetMapping("/api/games")
  public GameResponses.GamesListResponse listGames(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
      @RequestParam(required = false) String masterId,
      @RequestParam(required = false) String system,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset
  ) {
    return new GameResponses.GamesListResponse(schedule.listPublicGames(dateFrom, dateTo, masterId, system, limit, offset));
  }

  @GetMapping("/api/schedule")
  public GameResponses.GamesListResponse schedule(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
      @RequestParam(required = false) String masterId,
      @RequestParam(required = false) String system,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset
  ) {
    return listGames(dateFrom, dateTo, masterId, system, limit, offset);
  }

  @GetMapping("/api/games/{id}")
  public GameResponses.GameResponse getGame(@PathVariable String id) {
    return new GameResponses.GameResponse(schedule.getPublicGame(id));
  }

  @PostMapping("/api/game-signups")
  @ResponseStatus(HttpStatus.CREATED)
  public GameSignupResponse createSignup(@Valid @RequestBody GameSignupRequest request) {
    var result = schedule.createSignup(request);
    return new GameSignupResponse(
        true,
        "Запись сохранена. Мастер получил уведомление.",
        result.signupId(),
        result.game()
    );
  }
}
