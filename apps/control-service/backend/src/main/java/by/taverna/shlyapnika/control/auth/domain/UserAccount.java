package by.taverna.shlyapnika.control.auth.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "control_users")
public class UserAccount {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "public_id", nullable = false, unique = true)
  private String publicId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  @Column(name = "display_name", nullable = false)
  private String displayName;
  @Column(nullable = false, unique = true)
  private String email;
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;
  @Column(name = "avatar_url")
  private String avatarUrl;
  @Column(name = "telegram_username")
  private String telegramUsername;
  @Column(name = "telegram_user_id")
  private String telegramUserId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status = UserStatus.ACTIVE;
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "control_user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  private Set<UserRole> roles = EnumSet.of(UserRole.VIEWER);
  @Column(nullable = false)
  private String timezone = "Europe/Minsk";
  @Column(nullable = false)
  private String locale = "ru";
  @Column(name = "two_factor_enabled", nullable = false)
  private boolean twoFactorEnabled;
  @Column(name = "two_factor_secret_encrypted")
  private String twoFactorSecretEncrypted;
  @Column(name = "email_verified_at")
  private Instant emailVerifiedAt;
  @Column(name = "last_login_at")
  private Instant lastLoginAt;
  @Column(name = "deleted_at")
  private Instant deletedAt;
  @Version
  private Long version;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected UserAccount() {
  }

  public UserAccount(String displayName, String email, String passwordHash, Set<UserRole> roles) {
    this.displayName = displayName;
    this.email = email.toLowerCase();
    this.passwordHash = passwordHash;
    this.roles = EnumSet.copyOf(roles);
  }

  public UUID getId() { return id; }
  public String getPublicId() { return publicId; }
  public String getDisplayName() { return displayName; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getAvatarUrl() { return avatarUrl; }
  public String getTelegramUsername() { return telegramUsername; }
  public String getTelegramUserId() { return telegramUserId; }
  public UserStatus getStatus() { return status; }
  public Set<UserRole> getRoles() { return roles; }
  public String getTimezone() { return timezone; }
  public String getLocale() { return locale; }
  public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
  public String getTwoFactorSecretEncrypted() { return twoFactorSecretEncrypted; }
  public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
  public Instant getLastLoginAt() { return lastLoginAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }

  public void markLogin() {
    lastLoginAt = Instant.now();
    updatedAt = Instant.now();
  }

  public void verifyEmail() {
    emailVerifiedAt = Instant.now();
    updatedAt = Instant.now();
  }

  public void updateRoles(Set<UserRole> nextRoles) {
    roles = EnumSet.copyOf(nextRoles);
    updatedAt = Instant.now();
  }

  public void block() {
    status = UserStatus.BLOCKED;
    updatedAt = Instant.now();
  }
}
