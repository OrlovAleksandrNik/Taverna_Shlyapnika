package by.taverna.shlyapnika.internal.api;

import by.taverna.shlyapnika.internal.InternalService;
import by.taverna.shlyapnika.internal.api.InternalGalleryResponses.InternalGalleryListResponse;
import by.taverna.shlyapnika.internal.api.InternalGalleryResponses.InternalGalleryPostResponse;
import by.taverna.shlyapnika.internal.api.InternalMediaResponses.StoredMediaResponse;
import by.taverna.shlyapnika.internal.api.InternalRatingRequests.CreatePlayerRequest;
import by.taverna.shlyapnika.internal.api.InternalRatingRequests.GameResultRequest;
import by.taverna.shlyapnika.internal.api.InternalRatingRequests.InspirationAdjustmentRequest;
import by.taverna.shlyapnika.internal.api.InternalRatingRequests.PointsAdjustmentRequest;
import by.taverna.shlyapnika.internal.api.InternalRatingRequests.VisibilityRequest;
import by.taverna.shlyapnika.internal.api.InternalRatingResponses.InternalRatingHistoryResponse;
import by.taverna.shlyapnika.internal.api.InternalRatingResponses.InternalRatingMutationResponse;
import by.taverna.shlyapnika.internal.api.InternalRatingResponses.InternalRatingPlayerResponse;
import by.taverna.shlyapnika.internal.api.InternalRatingResponses.InternalRatingPlayersResponse;
import by.taverna.shlyapnika.schedule.api.GameResponses.GameResponse;
import by.taverna.shlyapnika.schedule.api.GameResponses.GamesListResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @GetMapping("/api/internal/masters/{masterId}/gallery-posts")
  public InternalGalleryListResponse listMasterGalleryPosts(@PathVariable String masterId) {
    return new InternalGalleryListResponse(service.listMasterGalleryPosts(masterId));
  }

  @PostMapping("/api/internal/masters/{masterId}/gallery-posts")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalGalleryPostResponse createMasterGalleryPost(@PathVariable String masterId, @Valid @RequestBody InternalGalleryPostRequest request) {
    return new InternalGalleryPostResponse(service.createMasterGalleryPost(masterId, request));
  }

  @PatchMapping("/api/internal/masters/{masterId}/gallery-posts/{postId}/status")
  public InternalGalleryPostResponse setMasterGalleryPostStatus(@PathVariable String masterId, @PathVariable String postId, @Valid @RequestBody StatusRequest request) {
    return new InternalGalleryPostResponse(service.setMasterGalleryPostStatus(masterId, postId, request.status()));
  }

  @PostMapping(value = "/api/internal/media/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public StoredMediaResponse uploadGalleryMedia(
      @RequestPart("file") MultipartFile file,
      @RequestParam(required = false) String namespace,
      @RequestParam(required = false) String altText
  ) throws IOException {
    return new StoredMediaResponse(service.storeGalleryMedia(namespace, altText, file.getOriginalFilename(), file.getContentType(), file.getBytes()));
  }

  @GetMapping("/api/internal/masters/{masterId}/rating/players")
  public InternalRatingPlayersResponse listRatingPlayers(@PathVariable String masterId, @RequestParam(defaultValue = "true") boolean includeHidden) {
    return new InternalRatingPlayersResponse(service.listRatingPlayers(masterId, includeHidden));
  }

  @GetMapping("/api/internal/masters/{masterId}/rating/history")
  public InternalRatingHistoryResponse listRatingHistory(@PathVariable String masterId, @RequestParam(defaultValue = "10") Integer limit) {
    return new InternalRatingHistoryResponse(service.listRatingHistory(masterId, limit));
  }

  @PostMapping("/api/internal/masters/{masterId}/rating/players")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalRatingPlayerResponse createRatingPlayer(@PathVariable String masterId, @Valid @RequestBody CreatePlayerRequest request) {
    return new InternalRatingPlayerResponse(service.createRatingPlayer(masterId, request.displayName(), request.nickname(), request.avatarUrl(), request.createdByTelegramId(), request.idempotencyKey()));
  }

  @PostMapping("/api/internal/masters/{masterId}/rating/game-results")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalRatingMutationResponse addRatingGameResult(@PathVariable String masterId, @Valid @RequestBody GameResultRequest request) {
    var result = service.addRatingGameResult(masterId, request.playerId(), request.points(), request.gameTitle(), request.gameDate(), request.masterName(), request.reason(), request.createdByTelegramId(), request.idempotencyKey());
    return new InternalRatingMutationResponse(result.eventId(), result.playedGameId());
  }

  @PostMapping("/api/internal/masters/{masterId}/rating/points")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalRatingMutationResponse adjustRatingPoints(@PathVariable String masterId, @Valid @RequestBody PointsAdjustmentRequest request) {
    var result = service.adjustRatingPoints(masterId, request.playerId(), request.pointsDelta(), request.reason(), request.createdByTelegramId(), request.idempotencyKey());
    return new InternalRatingMutationResponse(result.eventId(), result.playedGameId());
  }

  @PostMapping("/api/internal/masters/{masterId}/rating/inspiration")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalRatingMutationResponse adjustRatingInspiration(@PathVariable String masterId, @Valid @RequestBody InspirationAdjustmentRequest request) {
    var result = service.adjustRatingInspiration(masterId, request.playerId(), request.inspirationDelta(), request.reason(), request.createdByTelegramId(), request.idempotencyKey());
    return new InternalRatingMutationResponse(result.eventId(), result.playedGameId());
  }

  @PostMapping("/api/internal/masters/{masterId}/rating/visibility")
  @ResponseStatus(HttpStatus.CREATED)
  public InternalRatingMutationResponse setRatingPlayerVisibility(@PathVariable String masterId, @Valid @RequestBody VisibilityRequest request) {
    var result = service.setRatingPlayerVisibility(masterId, request.playerId(), request.isVisible(), request.reason(), request.createdByTelegramId(), request.idempotencyKey());
    return new InternalRatingMutationResponse(result.eventId(), result.playedGameId());
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
