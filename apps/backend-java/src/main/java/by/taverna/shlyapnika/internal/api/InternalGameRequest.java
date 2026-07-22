package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InternalGameRequest(
    @NotBlank String masterId,
    @NotBlank @Size(min = 3, max = 100) String title,
    @NotBlank @Size(min = 20, max = 1000) String description,
    @NotBlank @Size(min = 2, max = 80) String gameSystem,
    @NotBlank @Size(min = 2, max = 80) String experienceLevel,
    @NotBlank @Size(min = 2, max = 30) String ageRating,
    @NotBlank String date,
    @NotBlank String time,
    @Min(1) @Max(720) Integer durationMinutes,
    @NotNull @Min(1) @Max(20) Integer minPlayers,
    @NotNull @Min(1) @Max(20) Integer maxPlayers,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    String currency,
    @NotBlank String contactUrl
) {
}
