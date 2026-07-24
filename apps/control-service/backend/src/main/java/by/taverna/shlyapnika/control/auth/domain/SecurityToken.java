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
@Table(name = "control_security_tokens")
public class SecurityToken {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "user_id")
  private UUID userId;
  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;
  @Enumerated(EnumType.STRING)
  @Column(name = "token_type", nullable = false)
  private SecurityTokenType type;
  @Column(name = "target_value")
  private String targetValue;
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
  @Column(name = "used_at")
  private Instant usedAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected SecurityToken() {
  }

  public SecurityToken(UUID userId, String tokenHash, SecurityTokenType type, String targetValue, Instant expiresAt) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.type = type;
    this.targetValue = targetValue;
    this.expiresAt = expiresAt;
  }

  public UUID getUserId() { return userId; }
  public SecurityTokenType getType() { return type; }
  public String getTargetValue() { return targetValue; }

  public boolean isUsable(Instant now, SecurityTokenType expectedType) {
    return usedAt == null && type == expectedType && expiresAt.isAfter(now);
  }

  public void use() {
    usedAt = Instant.now();
  }
}
