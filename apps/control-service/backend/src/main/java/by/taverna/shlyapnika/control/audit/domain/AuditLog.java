package by.taverna.shlyapnika.control.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_audit_log")
public class AuditLog {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "actor_public_id")
  private String actorPublicId;
  @Column(nullable = false)
  private String action;
  @Column(name = "entity_type", nullable = false)
  private String entityType;
  @Column(name = "entity_id")
  private String entityId;
  @Column(columnDefinition = "TEXT")
  private String details;
  @Column(name = "ip_address")
  private String ipAddress;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected AuditLog() {
  }

  public AuditLog(String actorPublicId, String action, String entityType, String entityId, String details, String ipAddress) {
    this.actorPublicId = actorPublicId;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.details = details;
    this.ipAddress = ipAddress;
  }

  public UUID getId() { return id; }
  public String getActorPublicId() { return actorPublicId; }
  public String getAction() { return action; }
  public String getEntityType() { return entityType; }
  public String getEntityId() { return entityId; }
  public String getDetails() { return details; }
  public Instant getCreatedAt() { return createdAt; }
}
