package by.taverna.shlyapnika.control.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_sessions")
public class ControlSession {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "user_id", nullable = false)
  private UUID userId;
  @Column(name = "session_hash", nullable = false, unique = true)
  private String sessionHash;
  @Column(name = "user_agent")
  private String userAgent;
  @Column(name = "ip_address")
  private String ipAddress;
  @Column(name = "revoked_at")
  private Instant revokedAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected ControlSession() {
  }

  public ControlSession(UUID userId, String sessionHash, String userAgent, String ipAddress, Instant expiresAt) {
    this.userId = userId;
    this.sessionHash = sessionHash;
    this.userAgent = userAgent;
    this.ipAddress = ipAddress;
    this.expiresAt = expiresAt;
  }

  public UUID getId() { return id; }
  public String getUserAgent() { return userAgent; }
  public String getIpAddress() { return ipAddress; }
  public Instant getRevokedAt() { return revokedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getExpiresAt() { return expiresAt; }

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public void revoke() {
    revokedAt = Instant.now();
  }
}
