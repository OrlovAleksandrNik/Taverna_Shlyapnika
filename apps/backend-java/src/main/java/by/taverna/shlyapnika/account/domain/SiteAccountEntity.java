package by.taverna.shlyapnika.account.domain;

import by.taverna.shlyapnika.schedule.domain.ConsentSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "\"SiteAccount\"")
public class SiteAccountEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @Column(name = "\"displayName\"", nullable = false)
  private String displayName;

  @Column(name = "\"email\"", nullable = false)
  private String email;

  @Column(name = "\"passwordHash\"", nullable = false)
  private String passwordHash;

  @Column(name = "\"role\"", nullable = false)
  private String role;

  @Column(name = "\"status\"", nullable = false)
  private String status;

  @Column(name = "\"telegramUsername\"")
  private String telegramUsername;

  @Column(name = "\"normalizedTelegramUsername\"")
  private String normalizedTelegramUsername;

  @Column(name = "\"consentGiven\"", nullable = false)
  private boolean consentGiven;

  @Column(name = "\"consentVersion\"", nullable = false)
  private String consentVersion;

  @Column(name = "\"privacyPolicyVersion\"", nullable = false)
  private String privacyPolicyVersion;

  @Column(name = "\"consentedAt\"")
  private Instant consentedAt;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public static SiteAccountEntity create(
      String id,
      String displayName,
      String email,
      String passwordHash,
      String role,
      String status,
      String telegramUsername,
      String normalizedTelegramUsername,
      ConsentSnapshot consent
  ) {
    var now = Instant.now();
    var account = new SiteAccountEntity();
    account.id = id;
    account.displayName = displayName;
    account.email = email;
    account.passwordHash = passwordHash;
    account.role = role;
    account.status = status;
    account.telegramUsername = telegramUsername;
    account.normalizedTelegramUsername = normalizedTelegramUsername;
    account.consentGiven = true;
    account.consentVersion = consent.consentVersion();
    account.privacyPolicyVersion = consent.privacyPolicyVersion();
    account.consentedAt = consent.consentedAt();
    account.createdAt = now;
    account.updatedAt = now;
    return account;
  }

  public void approveMaster() {
    this.status = "active";
    this.updatedAt = Instant.now();
  }

  public void rejectMaster() {
    this.status = "rejected";
    this.updatedAt = Instant.now();
  }

  public String getId() { return id; }
  public String getDisplayName() { return displayName; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getRole() { return role; }
  public String getStatus() { return status; }
  public String getTelegramUsername() { return telegramUsername; }
  public String getNormalizedTelegramUsername() { return normalizedTelegramUsername; }
  public Instant getCreatedAt() { return createdAt; }
}
