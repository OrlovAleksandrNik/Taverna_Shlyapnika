package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackendGameUpdateRequest(
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
