package by.taverna.shlyapnika.audit.domain;

import by.taverna.shlyapnika.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "\"AuditLog\"")
public class AuditLogEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @Column(name = "\"userId\"")
  private String userId;

  @Column(name = "\"action\"", nullable = false)
  private String action;

  @Column(name = "\"entityType\"", nullable = false)
  private String entityType;

  @Column(name = "\"entityId\"")
  private String entityId;

  @Column(name = "\"details\"", columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private String details;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  public static AuditLogEntity of(String userId, String action, String entityType, String entityId, String details) {
    var entity = new AuditLogEntity();
    entity.userId = userId;
    entity.action = action;
    entity.entityType = entityType;
    entity.entityId = entityId;
    entity.details = details;
    return entity;
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("aud");
    if (createdAt == null) createdAt = Instant.now();
  }
}
