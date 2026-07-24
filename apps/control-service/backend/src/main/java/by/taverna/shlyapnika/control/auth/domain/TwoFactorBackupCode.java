package by.taverna.shlyapnika.control.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_two_factor_backup_codes")
public class TwoFactorBackupCode {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "user_id", nullable = false)
  private UUID userId;
  @Column(name = "code_hash", nullable = false)
  private String codeHash;
  @Column(name = "used_at")
  private Instant usedAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected TwoFactorBackupCode() {
  }

  public TwoFactorBackupCode(UUID userId, String codeHash) {
    this.userId = userId;
    this.codeHash = codeHash;
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public String getCodeHash() { return codeHash; }
  public boolean isUnused() { return usedAt == null; }

  public void use() {
    usedAt = Instant.now();
  }
}
