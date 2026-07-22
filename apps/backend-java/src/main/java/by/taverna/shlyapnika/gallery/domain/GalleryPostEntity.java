package by.taverna.shlyapnika.gallery.domain;

import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.master.domain.MasterEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "\"GalleryPost\"")
public class GalleryPostEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @Column(name = "\"publicId\"", nullable = false)
  private String publicId;

  @Column(name = "\"type\"", nullable = false)
  @ColumnTransformer(write = "?::\"GalleryPostType\"")
  private String type = "photo";

  @Column(name = "\"title\"", nullable = false)
  private String title;

  @Column(name = "\"description\"")
  private String description;

  @Column(name = "\"storyContent\"")
  private String storyContent;

  @Column(name = "\"storyHtml\"")
  private String storyHtml;

  @Column(name = "\"category\"", nullable = false)
  @ColumnTransformer(write = "?::\"GalleryCategory\"")
  private String category = "games";

  @Column(name = "\"eventDate\"")
  private Instant eventDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"authorMasterId\"")
  private MasterEntity authorMaster;

  @Column(name = "\"status\"", nullable = false)
  @ColumnTransformer(write = "?::\"GalleryPostStatus\"")
  private String status = "draft";

  @Column(name = "\"isVisible\"", nullable = false)
  private Boolean isVisible = true;

  @Column(name = "\"sortOrder\"", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "\"publishedAt\"")
  private Instant publishedAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "galleryPost", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder asc")
  private List<GalleryMediaEntity> media = new ArrayList<>();

  public static GalleryPostEntity create(
      String publicId,
      String type,
      String title,
      String description,
      String storyContent,
      String storyHtml,
      String category,
      Instant eventDate,
      MasterEntity authorMaster,
      String status,
      List<GalleryMediaEntity> media
  ) {
    var post = new GalleryPostEntity();
    post.publicId = publicId;
    post.type = type;
    post.title = title;
    post.description = description;
    post.storyContent = storyContent;
    post.storyHtml = storyHtml;
    post.category = category;
    post.eventDate = eventDate;
    post.authorMaster = authorMaster;
    post.setStatus(status);
    if (media != null) media.forEach(post::addMedia);
    return post;
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("gpo");
    var now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public void addMedia(GalleryMediaEntity item) {
    item.attachTo(this);
    media.add(item);
  }

  public void setStatus(String status) {
    this.status = status;
    this.isVisible = !"hidden".equals(status);
    if ("published".equals(status) && publishedAt == null) publishedAt = Instant.now();
  }

  public String getId() { return id; }
  public String getPublicId() { return publicId; }
  public String getType() { return type; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public String getStoryHtml() { return storyHtml; }
  public String getCategory() { return category; }
  public Instant getEventDate() { return eventDate; }
  public MasterEntity getAuthorMaster() { return authorMaster; }
  public String getStatus() { return status; }
  public Boolean getIsVisible() { return isVisible; }
  public Instant getPublishedAt() { return publishedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public List<GalleryMediaEntity> getMedia() { return media; }
}
