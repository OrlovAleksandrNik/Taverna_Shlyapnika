package by.taverna.shlyapnika.bot.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackendGameResponse(GameDto game) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GameDto(String id, String title, String status, String startsAtLabel, Integer minPlayers, Integer maxPlayers, int bookedSeats, int availableSeats) {
  }
}
