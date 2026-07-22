package by.taverna.shlyapnika.gallery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "\"GalleryMedia\"")
public class GalleryMediaEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "\"galleryPostId\"", nullable = false)
  private GalleryPostEntity galleryPost;

  @Column(name = "\"fileUrl\"", nullable = false)
  private String fileUrl;

  @Column(name = "\"thumbnailUrl\"", nullable = false)
  private String thumbnailUrl;

  @Column(name = "\"mediumUrl\"", nullable = false)
  private String mediumUrl;

  @Column(name = "\"width\"")
  private Integer width;

  @Column(name = "\"height\"")
  private Integer height;

  @Column(name = "\"mimeType\"", nullable = false)
  private String mimeType;

  @Column(name = "\"altText\"")
  private String altText;

  @Column(name = "\"sortOrder\"", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  public String getId() { return id; }
  public String getFileUrl() { return fileUrl; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public String getMediumUrl() { return mediumUrl; }
  public Integer getWidth() { return width; }
  public Integer getHeight() { return height; }
  public String getMimeType() { return mimeType; }
  public String getAltText() { return altText; }
}
