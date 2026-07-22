package by.taverna.shlyapnika.master.domain;

import by.taverna.shlyapnika.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "\"Master\"")
public class MasterEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @Column(name = "\"telegramUserId\"", nullable = false, unique = true)
  private Long telegramUserId;

  @Column(name = "\"telegramUsername\"")
  private String telegramUsername;

  @Column(name = "\"displayName\"", nullable = false)
  private String displayName;

  @Column(name = "\"contactUrl\"", nullable = false)
  private String contactUrl;

  @Column(name = "\"role\"", nullable = false)
  @ColumnTransformer(write = "?::\"MasterRole\"")
  private String role = "master";

  @Column(name = "\"status\"", nullable = false)
  @ColumnTransformer(write = "?::\"MasterStatus\"")
  private String status = "active";

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("mst");
    var now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public Long getTelegramUserId() {
    return telegramUserId;
  }

  public String getTelegramUsername() {
    return telegramUsername;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getContactUrl() {
    return contactUrl;
  }

  public String getRole() {
    return role;
  }

  public String getStatus() {
    return status;
  }
}
