package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InternalGameUpdateRequest(
    @Size(min = 3, max = 100) String title,
    @Size(min = 20, max = 1000) String description,
    @Size(min = 2, max = 80) String gameSystem,
    @Size(min = 2, max = 80) String experienceLevel,
    @Size(min = 2, max = 30) String ageRating,
    String date,
    String time,
    @Min(1) @Max(720) Integer durationMinutes,
    @Min(1) @Max(20) Integer minPlayers,
    @Min(1) @Max(20) Integer maxPlayers,
    @DecimalMin("0.00") BigDecimal price,
    String currency,
    @Size(min = 5, max = 120) String contactUrl
) {
}
