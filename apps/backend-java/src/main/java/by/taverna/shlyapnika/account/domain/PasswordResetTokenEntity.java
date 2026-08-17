package by.taverna.shlyapnika.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "\"PasswordResetToken\"")
public class PasswordResetTokenEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "\"accountId\"")
  private SiteAccountEntity account;

  @Column(name = "\"tokenHash\"", nullable = false)
  private String tokenHash;

  @Column(name = "\"expiresAt\"", nullable = false)
  private Instant expiresAt;

  @Column(name = "\"usedAt\"")
  private Instant usedAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  public static PasswordResetTokenEntity create(String id, SiteAccountEntity account, String tokenHash, Instant expiresAt) {
    var entity = new PasswordResetTokenEntity();
    entity.id = id;
    entity.account = account;
    entity.tokenHash = tokenHash;
    entity.expiresAt = expiresAt;
    entity.createdAt = Instant.now();
    return entity;
  }

  public boolean usable(Instant now) {
    return usedAt == null && expiresAt.isAfter(now);
  }

  public void markUsed() {
    this.usedAt = Instant.now();
  }

  public String getId() { return id; }
  public SiteAccountEntity getAccount() { return account; }
  public String getTokenHash() { return tokenHash; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getUsedAt() { return usedAt; }
}
