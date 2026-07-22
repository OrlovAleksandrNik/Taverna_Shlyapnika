package by.taverna.shlyapnika.gallery.api;

import by.taverna.shlyapnika.gallery.GalleryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GalleryController {
  private final GalleryService service;

  public GalleryController(GalleryService service) {
    this.service = service;
  }

  @GetMapping("/api/gallery/categories")
  public GalleryResponses.CategoriesResponse categories() {
    return new GalleryResponses.CategoriesResponse(List.of(
        new GalleryResponses.CategoryDto("games"),
        new GalleryResponses.CategoryDto("events"),
        new GalleryResponses.CategoryDto("heroes"),
        new GalleryResponses.CategoryDto("tavern"),
        new GalleryResponses.CategoryDto("miniatures"),
        new GalleryResponses.CategoryDto("other")
    ));
  }

  @GetMapping("/api/gallery")
  public GalleryResponses.GalleryListResponse list(
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset
  ) {
    return new GalleryResponses.GalleryListResponse(service.listPublicPosts(limit, offset));
  }

  @GetMapping("/api/gallery/{publicId}")
  public GalleryResponses.GalleryPostResponse get(@PathVariable String publicId) {
    return new GalleryResponses.GalleryPostResponse(service.getPublicPost(publicId));
  }
}
