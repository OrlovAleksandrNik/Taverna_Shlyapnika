package by.taverna.shlyapnika.bot.backend;

import java.math.BigDecimal;

public record BackendGameRequest(
    String masterId,
    String title,
    String description,
    String gameSystem,
    String experienceLevel,
    String ageRating,
    String date,
    String time,
    Integer durationMinutes,
    Integer minPlayers,
    Integer maxPlayers,
    BigDecimal price,
    String currency,
    String contactUrl
) {
}
