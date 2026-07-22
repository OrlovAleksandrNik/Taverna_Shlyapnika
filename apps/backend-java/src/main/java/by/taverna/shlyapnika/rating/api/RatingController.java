package by.taverna.shlyapnika.rating.api;

import by.taverna.shlyapnika.rating.RatingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RatingController {
  private final RatingService service;

  public RatingController(RatingService service) {
    this.service = service;
  }

  @GetMapping("/api/rating")
  public RatingResponses.RatingResponse rating(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset
  ) {
    return service.listPublicRating(search, limit, offset);
  }
}
