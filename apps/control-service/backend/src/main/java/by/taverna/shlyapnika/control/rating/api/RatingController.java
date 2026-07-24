package by.taverna.shlyapnika.control.rating.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rating")
@PreAuthorize("hasAuthority('rating.read')")
public class RatingController extends SectionStubController {
  public RatingController() { super("rating"); }
}
