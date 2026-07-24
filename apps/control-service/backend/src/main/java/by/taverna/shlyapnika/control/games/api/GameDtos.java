package by.taverna.shlyapnika.control.games.api;

import by.taverna.shlyapnika.control.games.domain.GameStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class GameDtos {
  private GameDtos() {
  }

  public record GameRequest(
      @NotBlank String title,
      @NotBlank String description,
      @NotBlank String gameSystem,
      @NotBlank String experienceLevel,
      @NotNull Instant startsAt,
      @Min(30) int durationMinutes,
      @Min(1) int minPlayers,
      @Min(1) int maxPlayers,
      @NotNull BigDecimal price,
      String masterPublicId,
      String staffNotes,
      Long version
  ) {
  }

  public record GameResponse(
      UUID id,
      String title,
      String description,
      String gameSystem,
      String experienceLevel,
      GameStatus status,
      String masterPublicId,
      Instant startsAt,
      int durationMinutes,
      int minPlayers,
      int maxPlayers,
      BigDecimal price,
      String staffNotes,
      Long version
  ) {
  }
}
