package by.taverna.shlyapnika.account;

import by.taverna.shlyapnika.account.infrastructure.PasswordResetTokenRepository;
import by.taverna.shlyapnika.account.infrastructure.SiteAccountRepository;
import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.Ids;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
  public static final String PUBLIC_RESPONSE = "Если аккаунт с таким email существует, мы отправили письмо для восстановления пароля.";
  private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(45);

  private final SiteAccountRepository accounts;
  private final PasswordResetTokenRepository tokens;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetMailer mailer;
  private final AuditService auditService;
  private final SecureRandom secureRandom;
  private final Clock clock;

  public PasswordResetService(
      SiteAccountRepository accounts,
      PasswordResetTokenRepository tokens,
      PasswordEncoder passwordEncoder,
      PasswordResetMailer mailer,
      AuditService auditService
  ) {
    this(accounts, tokens, passwordEncoder, mailer, auditService, new SecureRandom(), Clock.systemUTC());
  }

  PasswordResetService(
      SiteAccountRepository accounts,
      PasswordResetTokenRepository tokens,
      PasswordEncoder passwordEncoder,
      PasswordResetMailer mailer,
      AuditService auditService,
      SecureRandom secureRandom,
      Clock clock
  ) {
    this.accounts = accounts;
    this.tokens = tokens;
    this.passwordEncoder = passwordEncoder;
    this.mailer = mailer;
    this.auditService = auditService;
    this.secureRandom = secureRandom;
    this.clock = clock;
  }

  @Transactional
  public String requestReset(String email) {
    var normalizedEmail = normalizeEmail(email);
    accounts.findByEmailIgnoreCase(normalizedEmail)
        .filter(account -> "active".equals(account.getStatus()))
        .ifPresent(account -> {
          var rawToken = randomToken();
          var token = by.taverna.shlyapnika.account.domain.PasswordResetTokenEntity.create(
              Ids.newId("prt"),
              account,
              hashToken(rawToken),
              Instant.now(clock).plus(TOKEN_LIFETIME)
          );
          tokens.save(token);
          mailer.sendResetLink(account.getEmail(), rawToken);
          auditService.write(account.getId(), "account.password_reset_requested", "SiteAccount", account.getId(), null);
        });
    return PUBLIC_RESPONSE;
  }

  @Transactional
  public void resetPassword(String token, String password, String confirmation) {
    if (!password.equals(confirmation)) throw new IllegalArgumentException("Пароли не совпадают.");
    var resetToken = tokens.findByTokenHash(hashToken(token))
        .orElseThrow(() -> new IllegalArgumentException("Ссылка восстановления недействительна или устарела."));
    if (!resetToken.usable(Instant.now(clock))) {
      throw new IllegalArgumentException("Ссылка восстановления недействительна или устарела.");
    }
    var account = resetToken.getAccount();
    account.changePassword(passwordEncoder.encode(password));
    resetToken.markUsed();
    accounts.save(account);
    tokens.save(resetToken);
    auditService.write(account.getId(), "account.password_changed_by_reset", "SiteAccount", account.getId(), null);
  }

  static String hashToken(String rawToken) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception error) {
      throw new IllegalStateException("Не удалось подготовить токен восстановления.", error);
    }
  }

  private String randomToken() {
    var bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String normalizeEmail(String email) {
    return String.valueOf(email).trim().toLowerCase(Locale.ROOT);
  }
}
