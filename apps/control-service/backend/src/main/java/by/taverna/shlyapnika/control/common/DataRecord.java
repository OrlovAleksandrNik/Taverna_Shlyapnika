package by.taverna.shlyapnika.control.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_records")
public class DataRecord {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(nullable = false)
  private String section;
  @Column(name = "public_id", nullable = false, unique = true)
  private String publicId = "rec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  @Column(nullable = false)
  private String title;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DataRecordStatus status = DataRecordStatus.DRAFT;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String payload;
  @Column(name = "deleted_at")
  private Instant deletedAt;
  @Column(name = "published_at")
  private Instant publishedAt;
  @Version
  private Long version;
  @Column(name = "created_by")
  private String createdBy;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected DataRecord() {
  }

  public DataRecord(String section, String title, String payload, String createdBy) {
    this.section = section;
    this.title = title;
    this.payload = payload;
    this.createdBy = createdBy;
  }

  public UUID getId() { return id; }
  public String getSection() { return section; }
  public String getPublicId() { return publicId; }
  public String getTitle() { return title; }
  public DataRecordStatus getStatus() { return status; }
  public String getPayload() { return payload; }
  public Instant getDeletedAt() { return deletedAt; }
  public Instant getPublishedAt() { return publishedAt; }
  public Long getVersion() { return version; }
  public String getCreatedBy() { return createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void update(String title, String payload) {
    this.title = title;
    this.payload = payload;
    this.updatedAt = Instant.now();
  }

  public void publish() {
    this.status = DataRecordStatus.PUBLISHED;
    this.publishedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void archive() {
    this.status = DataRecordStatus.ARCHIVED;
    this.updatedAt = Instant.now();
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
    this.status = DataRecordStatus.ARCHIVED;
    this.updatedAt = Instant.now();
  }
}
