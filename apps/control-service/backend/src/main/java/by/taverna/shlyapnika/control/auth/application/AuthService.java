package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.auth.domain.Invitation;
import by.taverna.shlyapnika.control.auth.domain.LoginHistory;
import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.auth.domain.UserRole;
import by.taverna.shlyapnika.control.auth.domain.UserStatus;
import by.taverna.shlyapnika.control.auth.infrastructure.InvitationRepository;
import by.taverna.shlyapnika.control.auth.infrastructure.LoginHistoryRepository;
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
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserAccountRepository users;
  private final InvitationRepository invitations;
  private final LoginHistoryRepository loginHistory;
  private final PasswordEncoder passwordEncoder;
  private final AuditService audit;
  private final ControlProperties properties;
  private final SecureRandom random = new SecureRandom();

  public AuthService(UserAccountRepository users, InvitationRepository invitations, LoginHistoryRepository loginHistory,
      PasswordEncoder passwordEncoder, AuditService audit, ControlProperties properties) {
    this.users = users;
    this.invitations = invitations;
    this.loginHistory = loginHistory;
    this.passwordEncoder = passwordEncoder;
    this.audit = audit;
    this.properties = properties;
  }

  @Transactional
  public InvitationResult createInvitation(UserAccount actor, String email, String displayName, UserRole role, String ipAddress) {
    if (users.existsByEmail(email.toLowerCase())) {
      throw new IllegalArgumentException("Пользователь с таким email уже существует.");
    }
    String token = randomToken();
    Invitation invitation = invitations.save(new Invitation(sha256(token), email, displayName, role, Instant.now().plus(Duration.ofDays(3)), actor.getId()));
    audit.record(actor.getPublicId(), "users.invite", "Invitation", invitation.getId().toString(), "role=" + role, ipAddress);
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
  public UserAccount authenticate(String email, String password, String ipAddress, String userAgent) {
    UserAccount account = users.findByEmail(email.toLowerCase()).orElse(null);
    boolean ok = account != null
        && account.getStatus() == UserStatus.ACTIVE
        && passwordEncoder.matches(password, account.getPasswordHash());
    loginHistory.save(new LoginHistory(account == null ? null : account.getId(), email, ok, ok ? "ok" : "invalid_credentials", ipAddress, userAgent));
    if (!ok) {
      throw new IllegalArgumentException("Неверный email или пароль.");
    }
    account.markLogin();
    audit.record(account.getPublicId(), "auth.login", "UserAccount", account.getPublicId(), "session-cookie", ipAddress);
    return account;
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

  public static String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public record InvitationResult(String id, String oneTimeToken, Instant expiresAt) {
  }
}
