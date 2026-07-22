package by.taverna.shlyapnika.internal.api;

import by.taverna.shlyapnika.rating.api.RatingResponses.InternalRatingEventDto;
import by.taverna.shlyapnika.rating.api.RatingResponses.InternalRatingPlayerDto;
import java.util.List;

public final class InternalRatingResponses {
  private InternalRatingResponses() {
  }

  public record InternalRatingPlayersResponse(List<InternalRatingPlayerDto> players) {
  }

  public record InternalRatingHistoryResponse(List<InternalRatingEventDto> events) {
  }

  public record InternalRatingPlayerResponse(InternalRatingPlayerDto player) {
  }

  public record InternalRatingMutationResponse(String eventId, String playedGameId) {
  }
}
