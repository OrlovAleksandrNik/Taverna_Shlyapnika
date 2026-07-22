package by.taverna.shlyapnika.internal.api;

import by.taverna.shlyapnika.internal.InternalService;
import by.taverna.shlyapnika.schedule.api.GameResponses.GameResponse;
import by.taverna.shlyapnika.schedule.api.GameResponses.GamesListResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalController {
  private final InternalService service;

  public InternalController(InternalService service) {
    this.service = service;
  }

  @GetMapping("/api/internal/masters/by-telegram/{telegramUserId}")
  public InternalMasterResponse getMasterByTelegram(@PathVariable Long telegramUserId) {
    return service.getMasterByTelegram(telegramUserId);
  }

  @PostMapping("/api/internal/masters")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalMasterResponse upsertMaster(@Valid @RequestBody InternalMasterRequest request) {
    return service.upsertMaster(request);
  }

  @GetMapping("/api/internal/bot-sessions/{telegramUserId}")
  public InternalBotSessionResponse getBotSession(@PathVariable Long telegramUserId) {
    return service.getBotSession(telegramUserId);
  }

  @PostMapping("/api/internal/bot-sessions/{telegramUserId}")
  public InternalBotSessionResponse saveBotSession(@PathVariable Long telegramUserId, @Valid @RequestBody InternalBotSessionRequest request) {
    return service.saveBotSession(telegramUserId, request);
  }

  @DeleteMapping("/api/internal/bot-sessions/{telegramUserId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteBotSession(@PathVariable Long telegramUserId) {
    service.deleteBotSession(telegramUserId);
  }

  @GetMapping("/api/internal/masters/{masterId}/games")
  public GamesListResponse listMasterGames(@PathVariable String masterId, @RequestParam(required = false) String scope) {
    return new GamesListResponse(service.listMasterGames(masterId, scope));
  }

  @PatchMapping("/api/internal/masters/{masterId}/games/{gameId}/status")
  public GameResponse setMasterGameStatus(@PathVariable String masterId, @PathVariable String gameId, @Valid @RequestBody StatusRequest request) {
    return new GameResponse(service.setMasterGameStatus(masterId, gameId, request.status()));
  }

  @PatchMapping("/api/internal/masters/{masterId}/games/{gameId}")
  public GameResponse updateMasterGame(@PathVariable String masterId, @PathVariable String gameId, @Valid @RequestBody InternalGameUpdateRequest request) {
    return new GameResponse(service.updateMasterGame(masterId, gameId, request));
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
