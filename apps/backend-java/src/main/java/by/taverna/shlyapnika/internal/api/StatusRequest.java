package by.taverna.shlyapnika.internal.api;

import jakarta.validation.constraints.NotBlank;

public record StatusRequest(@NotBlank String status) {
}
