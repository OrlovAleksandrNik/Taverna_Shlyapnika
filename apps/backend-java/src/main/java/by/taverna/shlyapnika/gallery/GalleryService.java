package by.taverna.shlyapnika.gallery;

import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.gallery.api.GalleryResponses.GalleryPostDto;
import by.taverna.shlyapnika.gallery.api.GalleryResponses.MasterDto;
import by.taverna.shlyapnika.gallery.api.GalleryResponses.MediaDto;
import by.taverna.shlyapnika.gallery.domain.GalleryPostEntity;
import by.taverna.shlyapnika.gallery.infrastructure.GalleryPostRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GalleryService {
  private static final Logger log = LoggerFactory.getLogger(GalleryService.class);

  private final GalleryPostRepository repository;

  public GalleryService(GalleryPostRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<GalleryPostDto> listPublicPosts(Integer limit, Integer offset) {
    var posts = repository.findPublicPosts(Math.min(limit == null ? 60 : limit, 100), Math.max(offset == null ? 0 : offset, 0))
        .stream()
        .map(this::toDto)
        .toList();
    log.info("public gallery requested count={}", posts.size());
    return posts;
  }

  @Transactional(readOnly = true)
  public GalleryPostDto getPublicPost(String publicId) {
    return repository.findPublicPost(publicId).map(this::toDto)
        .orElseThrow(() -> new NotFoundException("Публикация не найдена."));
  }

  private GalleryPostDto toDto(GalleryPostEntity post) {
    var master = post.getAuthorMaster() == null
        ? null
        : new MasterDto(post.getAuthorMaster().getId(), post.getAuthorMaster().getDisplayName());
    var media = post.getMedia().stream()
        .map(item -> new MediaDto(
            item.getId(),
            item.getFileUrl(),
            item.getThumbnailUrl(),
            item.getMediumUrl(),
            item.getWidth(),
            item.getHeight(),
            item.getMimeType(),
            item.getAltText()
        ))
        .toList();
    return new GalleryPostDto(
        post.getId(),
        post.getPublicId(),
        post.getType(),
        post.getTitle(),
        post.getDescription(),
        post.getStoryHtml(),
        post.getCategory(),
        post.getEventDate(),
        master,
        media,
        post.getCreatedAt(),
        post.getPublishedAt()
    );
  }
}
