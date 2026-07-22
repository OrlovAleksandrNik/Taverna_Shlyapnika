package by.taverna.shlyapnika.schedule.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class GameResponses {
  private GameResponses() {
  }

  public record MasterDto(String id, String name, String contactUrl, String telegramUsername) {
  }

  public record PublicGameDto(
      String id,
      String title,
      String description,
      String system,
      String gameSystem,
      String systemName,
      String experienceLevel,
      String ageRating,
      Instant dateTimeStart,
      String startsAtLabel,
      Integer durationMinutes,
      Instant dateTimeEnd,
      Integer minPlayers,
      Integer maxPlayers,
      BigDecimal price,
      String currency,
      String imageUrl,
      String contactUrl,
      String status,
      String masterId,
      String masterName,
      String masterTelegramUsername,
      MasterDto master,
      int bookedSeats,
      int availableSeats,
      List<String> tags
  ) {
  }

  public record GamesListResponse(List<PublicGameDto> games) {
  }

  public record GameResponse(PublicGameDto game) {
  }
}
