package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.auth.domain.Invitation;
import by.taverna.shlyapnika.control.auth.domain.LoginHistory;
import by.taverna.shlyapnika.control.auth.domain.ControlSession;
import by.taverna.shlyapnika.control.auth.domain.SecurityToken;
import by.taverna.shlyapnika.control.auth.domain.SecurityTokenType;
import by.taverna.shlyapnika.control.auth.domain.TwoFactorBackupCode;
import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.auth.domain.UserRole;
import by.taverna.shlyapnika.control.auth.domain.UserStatus;
import by.taverna.shlyapnika.control.auth.infrastructure.ControlSessionRepository;
import by.taverna.shlyapnika.control.auth.infrastructure.InvitationRepository;
import by.taverna.shlyapnika.control.auth.infrastructure.LoginHistoryRepository;
import by.taverna.shlyapnika.control.auth.infrastructure.SecurityTokenRepository;
import by.taverna.shlyapnika.control.auth.infrastructure.TwoFactorBackupCodeRepository;
import by.taverna.shlyapnika.control.auth.infrastructure.UserAccountRepository;
import by.taverna.shlyapnika.control.config.ControlProperties;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserAccountRepository users;
  private final InvitationRepository invitations;
  private final LoginHistoryRepository loginHistory;
  private final SecurityTokenRepository securityTokens;
  private final TwoFactorBackupCodeRepository backupCodes;
  private final ControlSessionRepository sessions;
  private final PasswordEncoder passwordEncoder;
  private final AuditService audit;
  private final ControlProperties properties;
  private final EmailGateway emailGateway;
  private final SecretEncryptionService encryption;
  private final TotpService totp;
  private final SecureRandom random = new SecureRandom();

  public AuthService(UserAccountRepository users, InvitationRepository invitations, LoginHistoryRepository loginHistory,
      SecurityTokenRepository securityTokens, TwoFactorBackupCodeRepository backupCodes, ControlSessionRepository sessions,
      PasswordEncoder passwordEncoder, AuditService audit, ControlProperties properties, EmailGateway emailGateway,
      SecretEncryptionService encryption, TotpService totp) {
    this.users = users;
    this.invitations = invitations;
    this.loginHistory = loginHistory;
    this.securityTokens = securityTokens;
    this.backupCodes = backupCodes;
    this.sessions = sessions;
    this.passwordEncoder = passwordEncoder;
    this.audit = audit;
    this.properties = properties;
    this.emailGateway = emailGateway;
    this.encryption = encryption;
    this.totp = totp;
  }

  @Transactional
  public InvitationResult createInvitation(UserAccount actor, String email, String displayName, UserRole role, String ipAddress) {
    if (users.existsByEmail(email.toLowerCase())) {
      throw new IllegalArgumentException("Пользователь с таким email уже существует.");
    }
    String token = randomToken();
    Invitation invitation = invitations.save(new Invitation(sha256(token), email, displayName, role, Instant.now().plus(Duration.ofDays(3)), actor.getId()));
    audit.record(actor.getPublicId(), "users.invite", "Invitation", invitation.getId().toString(), "role=" + role, ipAddress);
    emailGateway.sendInvitation(email, displayName, token, invitation.getExpiresAt());
    return new InvitationResult(invitation.getId().toString(), token, invitation.getExpiresAt());
  }

  @Transactional
  public UserAccount acceptInvitation(String token, String password) {
    Invitation invitation = invitations.findByTokenHash(sha256(token))
        .filter(invite -> invite.isUsable(Instant.now()))
        .orElseThrow(() -> new IllegalArgumentException("Приглашение недействительно или истекло."));
    if (users.existsByEmail(invitation.getEmail())) {
      throw new IllegalArgumentException("Пользователь уже создан.");
    }
    UserAccount account = new UserAccount(invitation.getDisplayName(), invitation.getEmail(), passwordEncoder.encode(password), Set.of(invitation.getRole()));
    account.verifyEmail();
    invitation.accept();
    UserAccount saved = users.save(account);
    audit.record(saved.getPublicId(), "auth.accept_invitation", "UserAccount", saved.getPublicId(), "role=" + invitation.getRole(), null);
    return saved;
  }

  @Transactional
  public UserAccount authenticate(String email, String password, String twoFactorCode, String ipAddress, String userAgent) {
    UserAccount account = users.findByEmail(email.toLowerCase()).orElse(null);
    boolean ok = account != null
        && account.getStatus() == UserStatus.ACTIVE
        && passwordEncoder.matches(password, account.getPasswordHash());
    if (ok && account.isTwoFactorEnabled()) {
      ok = verifySecondFactor(account, twoFactorCode);
    }
    loginHistory.save(new LoginHistory(account == null ? null : account.getId(), email, ok, ok ? "ok" : "invalid_credentials", ipAddress, userAgent));
    if (!ok) {
      throw new IllegalArgumentException("Неверный email или пароль.");
    }
    account.markLogin();
    audit.record(account.getPublicId(), "auth.login", "UserAccount", account.getPublicId(), "session-cookie", ipAddress);
    return account;
  }

  @Transactional
  public TokenResult requestPasswordReset(String email, String ipAddress) {
    UserAccount account = users.findByEmail(email.toLowerCase()).orElse(null);
    if (account == null || account.getStatus() != UserStatus.ACTIVE) {
      return TokenResult.hidden();
    }
    String token = createSecurityToken(account.getId(), SecurityTokenType.PASSWORD_RESET, null, Duration.ofHours(1));
    emailGateway.sendPasswordReset(account.getEmail(), token, Instant.now().plus(Duration.ofHours(1)));
    audit.record(account.getPublicId(), "auth.password_reset_requested", "UserAccount", account.getPublicId(), "mock email queued", ipAddress);
    return new TokenResult(true, token);
  }

  @Transactional
  public void resetPassword(String token, String nextPassword, String ipAddress) {
    SecurityToken securityToken = consumeToken(token, SecurityTokenType.PASSWORD_RESET);
    UserAccount account = users.findById(securityToken.getUserId()).orElseThrow(() -> new IllegalArgumentException("Invalid token."));
    account.changePassword(passwordEncoder.encode(nextPassword));
    audit.record(account.getPublicId(), "auth.password_reset_completed", "UserAccount", account.getPublicId(), "password hash rotated", ipAddress);
    emailGateway.sendSecurityAlert(account.getEmail(), "Password changed", "Your Taverna Control password was changed.");
  }

  @Transactional
  public TokenResult requestEmailVerification(UserAccount account, String ipAddress) {
    account = managed(account);
    String token = createSecurityToken(account.getId(), SecurityTokenType.EMAIL_VERIFICATION, account.getEmail(), Duration.ofDays(1));
    emailGateway.sendEmailVerification(account.getEmail(), token, Instant.now().plus(Duration.ofDays(1)));
    audit.record(account.getPublicId(), "auth.email_verification_requested", "UserAccount", account.getPublicId(), "mock email queued", ipAddress);
    return new TokenResult(true, token);
  }

  @Transactional
  public void verifyEmail(String token, String ipAddress) {
    SecurityToken securityToken = consumeToken(token, SecurityTokenType.EMAIL_VERIFICATION);
    UserAccount account = users.findById(securityToken.getUserId()).orElseThrow(() -> new IllegalArgumentException("Invalid token."));
    account.verifyEmail();
    audit.record(account.getPublicId(), "auth.email_verified", "UserAccount", account.getPublicId(), "email verified", ipAddress);
  }

  @Transactional
  public TwoFactorSetup startTwoFactorSetup(UserAccount account, String ipAddress) {
    account = managed(account);
    String secret = totp.generateSecret();
    account.configureTwoFactorSecret(encryption.encrypt(secret));
    audit.record(account.getPublicId(), "auth.2fa_setup_started", "UserAccount", account.getPublicId(), "secret encrypted", ipAddress);
    return new TwoFactorSetup(secret, totp.otpauthUrl("Taverna Control", account.getEmail(), secret));
  }

  @Transactional
  public List<String> confirmTwoFactor(UserAccount account, String code, String ipAddress) {
    account = managed(account);
    String encryptedSecret = account.getTwoFactorSecretEncrypted();
    if (encryptedSecret == null || encryptedSecret.isBlank()) {
      throw new IllegalArgumentException("Two-factor setup was not started.");
    }
    if (!totp.verify(encryption.decrypt(encryptedSecret), code)) {
      throw new IllegalArgumentException("Invalid two-factor code.");
    }
    account.enableTwoFactor();
    backupCodes.deleteByUserId(account.getId());
    List<String> plainCodes = generateBackupCodes();
    plainCodes.forEach(plain -> backupCodes.save(new TwoFactorBackupCode(account.getId(), passwordEncoder.encode(plain))));
    audit.record(account.getPublicId(), "auth.2fa_enabled", "UserAccount", account.getPublicId(), "backup codes hashed", ipAddress);
    return plainCodes;
  }

  @Transactional
  public void disableTwoFactor(UserAccount account, String password, String code, String ipAddress) {
    account = managed(account);
    if (!passwordEncoder.matches(password, account.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials.");
    }
    if (account.isTwoFactorEnabled() && !verifySecondFactor(account, code)) {
      throw new IllegalArgumentException("Invalid two-factor code.");
    }
    account.disableTwoFactor();
    backupCodes.deleteByUserId(account.getId());
    audit.record(account.getPublicId(), "auth.2fa_disabled", "UserAccount", account.getPublicId(), "backup codes removed", ipAddress);
  }

  @Transactional
  public void recordSession(UserAccount account, String rawSessionId, String userAgent, String ipAddress) {
    sessions.save(new ControlSession(account.getId(), sha256(rawSessionId), userAgent, ipAddress, Instant.now().plus(Duration.ofHours(12))));
  }

  @Transactional
  public List<ControlSession> sessionsFor(UserAccount account) {
    account = managed(account);
    return sessions.findByUserIdOrderByCreatedAtDesc(account.getId());
  }

  @Transactional
  public void revokeAllSessions(UserAccount account, String ipAddress) {
    account = managed(account);
    sessions.findByUserIdOrderByCreatedAtDesc(account.getId()).forEach(ControlSession::revoke);
    audit.record(account.getPublicId(), "auth.sessions_revoked", "UserAccount", account.getPublicId(), "all known sessions revoked", ipAddress);
  }

  @Transactional
  public void bootstrapOwnerIfConfigured() {
    if (properties.bootstrapOwnerEmail() == null || properties.bootstrapOwnerEmail().isBlank()) return;
    if (properties.bootstrapToken() == null || properties.bootstrapToken().length() < 16) return;
    if (users.countByRole(UserRole.OWNER) > 0) return;
    UserAccount owner = new UserAccount("Первичный владелец", properties.bootstrapOwnerEmail(), passwordEncoder.encode(properties.bootstrapToken()), Set.of(UserRole.OWNER));
    owner.verifyEmail();
    users.save(owner);
    audit.record(owner.getPublicId(), "bootstrap.owner_created", "UserAccount", owner.getPublicId(), "bootstrap disabled after first OWNER presence", null);
  }

  public String randomToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String createSecurityToken(UUID userId, SecurityTokenType type, String targetValue, Duration ttl) {
    String token = randomToken();
    securityTokens.save(new SecurityToken(userId, sha256(token), type, targetValue, Instant.now().plus(ttl)));
    return token;
  }

  private SecurityToken consumeToken(String token, SecurityTokenType expectedType) {
    SecurityToken securityToken = securityTokens.findByTokenHash(sha256(token))
        .filter(found -> found.isUsable(Instant.now(), expectedType))
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));
    securityToken.use();
    return securityToken;
  }

  private boolean verifySecondFactor(UserAccount account, String code) {
    if (code == null || code.isBlank()) return false;
    if (account.getTwoFactorSecretEncrypted() != null && totp.verify(encryption.decrypt(account.getTwoFactorSecretEncrypted()), code)) {
      return true;
    }
    return backupCodes.findByUserIdAndUsedAtIsNull(account.getId()).stream()
        .filter(stored -> passwordEncoder.matches(code, stored.getCodeHash()))
        .findFirst()
        .map(stored -> {
          stored.use();
          return true;
        })
        .orElse(false);
  }

  private List<String> generateBackupCodes() {
    return java.util.stream.IntStream.range(0, 10)
        .mapToObj(index -> randomToken().substring(0, 10).toUpperCase())
        .toList();
  }

  private UserAccount managed(UserAccount account) {
    return users.findById(account.getId()).orElseThrow(() -> new IllegalArgumentException("Account not found."));
  }

  public static String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public record InvitationResult(String id, String oneTimeToken, Instant expiresAt) {
  }

  public record TokenResult(boolean issued, String devOnlyToken) {
    static TokenResult hidden() {
      return new TokenResult(false, null);
    }
  }

  public record TwoFactorSetup(String secret, String otpauthUrl) {
  }
}
