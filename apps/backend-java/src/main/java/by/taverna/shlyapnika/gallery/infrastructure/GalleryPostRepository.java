package by.taverna.shlyapnika.gallery.infrastructure;

import by.taverna.shlyapnika.gallery.domain.GalleryPostEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GalleryPostRepository extends JpaRepository<GalleryPostEntity, String> {
  @EntityGraph(attributePaths = {"authorMaster", "media"})
  @Query(value = """
      select * from "GalleryPost"
      where "status" = cast('published' as "GalleryPostStatus")
        and "isVisible" = true
      order by "sortOrder" asc, "eventDate" desc nulls last, "createdAt" desc
      limit :limit offset :offset
      """, nativeQuery = true)
  List<GalleryPostEntity> findPublicPosts(@Param("limit") int limit, @Param("offset") int offset);

  @EntityGraph(attributePaths = {"authorMaster", "media"})
  @Query(value = """
      select * from "GalleryPost"
      where "publicId" = :publicId
        and "status" = cast('published' as "GalleryPostStatus")
        and "isVisible" = true
      limit 1
      """, nativeQuery = true)
  Optional<GalleryPostEntity> findPublicPost(@Param("publicId") String publicId);

  @EntityGraph(attributePaths = {"authorMaster", "media"})
  List<GalleryPostEntity> findTop12ByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = {"authorMaster", "media"})
  List<GalleryPostEntity> findTop12ByAuthorMaster_IdOrderByCreatedAtDesc(String masterId);

  @EntityGraph(attributePaths = {"authorMaster", "media"})
  Optional<GalleryPostEntity> findByIdAndAuthorMaster_Id(String id, String masterId);
}
