package by.taverna.shlyapnika.control.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_invitations")
public class Invitation {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;
  @Column(nullable = false)
  private String email;
  @Column(name = "display_name", nullable = false)
  private String displayName;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
  @Column(name = "accepted_at")
  private Instant acceptedAt;
  @Column(name = "created_by")
  private UUID createdBy;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected Invitation() {
  }

  public Invitation(String tokenHash, String email, String displayName, UserRole role, Instant expiresAt, UUID createdBy) {
    this.tokenHash = tokenHash;
    this.email = email.toLowerCase();
    this.displayName = displayName;
    this.role = role;
    this.expiresAt = expiresAt;
    this.createdBy = createdBy;
  }

  public UUID getId() { return id; }
  public String getTokenHash() { return tokenHash; }
  public String getEmail() { return email; }
  public String getDisplayName() { return displayName; }
  public UserRole getRole() { return role; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getAcceptedAt() { return acceptedAt; }
  public Instant getCreatedAt() { return createdAt; }

  public boolean isUsable(Instant now) {
    return acceptedAt == null && expiresAt.isAfter(now);
  }

  public void accept() {
    acceptedAt = Instant.now();
  }
}
