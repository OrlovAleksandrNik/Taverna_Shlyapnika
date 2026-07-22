package by.taverna.shlyapnika.internal.api;

import by.taverna.shlyapnika.internal.InternalService;
import by.taverna.shlyapnika.schedule.api.GameResponses.GameResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalController {
  private final InternalService service;

  public InternalController(InternalService service) {
    this.service = service;
  }

  @PostMapping("/api/internal/games")
  @ResponseStatus(HttpStatus.CREATED)
  public GameResponse createGame(@Valid @RequestBody InternalGameRequest request) {
    return new GameResponse(service.createGame(request));
  }

  @PatchMapping("/api/internal/games/{id}/status")
  public GameResponse setStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request) {
    return new GameResponse(service.setGameStatus(id, request.status()));
  }

  @PostMapping("/api/internal/archive-past-games")
  public Map<String, Object> archivePastGames() {
    return Map.of("archived", service.archivePastGames());
  }

  @PostMapping("/api/internal/privacy/withdraw-consent")
  public Map<String, Object> withdrawConsent(@Valid @RequestBody WithdrawConsentRequest request) {
    return Map.of("updated", service.withdrawConsent(request.entityType(), request.requestId(), Boolean.TRUE.equals(request.anonymize())));
  }
}
