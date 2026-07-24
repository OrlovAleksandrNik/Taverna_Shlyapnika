package by.taverna.shlyapnika.control.games.api;

import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.games.api.GameDtos.GameRequest;
import by.taverna.shlyapnika.control.games.api.GameDtos.GameResponse;
import by.taverna.shlyapnika.control.games.domain.ControlGame;
import by.taverna.shlyapnika.control.games.infrastructure.ControlGameRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/games")
public class GamesController {
  private final ControlGameRepository games;
  private final AuditService audit;

  public GamesController(ControlGameRepository games, AuditService audit) {
    this.games = games;
    this.audit = audit;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('games.read')")
  List<GameResponse> list() {
    return games.findTop20ByDeletedAtIsNullOrderByStartsAtAsc().stream().map(GamesController::toResponse).toList();
  }

  @PostMapping
  @PreAuthorize("hasAuthority('games.create')")
  GameResponse create(@Valid @RequestBody GameRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    ControlGame game = new ControlGame(request.title(), request.description(), request.gameSystem(), request.experienceLevel(),
        request.startsAt(), request.durationMinutes(), request.minPlayers(), request.maxPlayers(), request.price());
    game.update(request.title(), request.description(), request.gameSystem(), request.experienceLevel(), request.startsAt(),
        request.durationMinutes(), request.minPlayers(), request.maxPlayers(), request.price(), request.masterPublicId(), request.staffNotes());
    ControlGame saved = games.save(game);
    audit.record(authentication.getName(), "games.create", "ControlGame", saved.getId().toString(), saved.getTitle(), servletRequest.getRemoteAddr());
    return toResponse(saved);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('games.update')")
  GameResponse update(@PathVariable UUID id, @Valid @RequestBody GameRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    ControlGame game = games.findById(id).orElseThrow(() -> new IllegalArgumentException("Игра не найдена."));
    if (request.version() != null && !request.version().equals(game.getVersion())) {
      throw new IllegalArgumentException("Запись изменилась. Обновите данные перед сохранением.");
    }
    game.update(request.title(), request.description(), request.gameSystem(), request.experienceLevel(), request.startsAt(),
        request.durationMinutes(), request.minPlayers(), request.maxPlayers(), request.price(), request.masterPublicId(), request.staffNotes());
    audit.record(authentication.getName(), "games.update", "ControlGame", id.toString(), game.getTitle(), servletRequest.getRemoteAddr());
    return toResponse(games.save(game));
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAuthority('games.publish')")
  GameResponse publish(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    ControlGame game = games.findById(id).orElseThrow(() -> new IllegalArgumentException("Игра не найдена."));
    game.publish();
    audit.record(authentication.getName(), "games.publish", "ControlGame", id.toString(), game.getTitle(), servletRequest.getRemoteAddr());
    return toResponse(games.save(game));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('games.update')")
  GameResponse cancel(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    ControlGame game = games.findById(id).orElseThrow(() -> new IllegalArgumentException("Игра не найдена."));
    game.cancel();
    audit.record(authentication.getName(), "games.cancel", "ControlGame", id.toString(), game.getTitle(), servletRequest.getRemoteAddr());
    return toResponse(games.save(game));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('games.delete')")
  void delete(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    ControlGame game = games.findById(id).orElseThrow(() -> new IllegalArgumentException("Игра не найдена."));
    game.softDelete();
    games.save(game);
    audit.record(authentication.getName(), "games.soft_delete", "ControlGame", id.toString(), game.getTitle(), servletRequest.getRemoteAddr());
  }

  public static GameResponse toResponse(ControlGame game) {
    return new GameResponse(game.getId(), game.getTitle(), game.getDescription(), game.getGameSystem(), game.getExperienceLevel(),
        game.getStatus(), game.getMasterPublicId(), game.getStartsAt(), game.getDurationMinutes(), game.getMinPlayers(),
        game.getMaxPlayers(), game.getPrice(), game.getStaffNotes(), game.getVersion());
  }
}
