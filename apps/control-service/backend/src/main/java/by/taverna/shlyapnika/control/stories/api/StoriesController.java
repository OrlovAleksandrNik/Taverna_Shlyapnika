package by.taverna.shlyapnika.control.stories.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stories")
@PreAuthorize("hasAuthority('gallery.read')")
public class StoriesController extends SectionStubController {
  public StoriesController() { super("stories"); }
}
