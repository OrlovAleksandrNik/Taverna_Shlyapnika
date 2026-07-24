package by.taverna.shlyapnika.control.gallery.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/gallery")
@PreAuthorize("hasAuthority('gallery.read')")
public class GalleryController extends SectionStubController {
  public GalleryController() { super("gallery"); }
}
