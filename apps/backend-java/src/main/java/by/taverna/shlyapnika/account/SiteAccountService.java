package by.taverna.shlyapnika.account;

import by.taverna.shlyapnika.account.api.SiteAuthRequests.LoginRequest;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.RegisterRequest;
import by.taverna.shlyapnika.account.domain.SiteAccountEntity;
import by.taverna.shlyapnika.account.infrastructure.SiteAccountRepository;
import by.taverna.shlyapnika.access.MasterAccessService;
import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.consent.ConsentService;
import by.taverna.shlyapnika.notification.TelegramNotificationService;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteAccountService {
  private final SiteAccountRepository accounts;
  private final ConsentService consentService;
  private final PasswordEncoder passwordEncoder;
  private final TelegramNotificationService notifications;
  private final AuditService auditService;

  public SiteAccountService(
      SiteAccountRepository accounts,
      ConsentService consentService,
      PasswordEncoder passwordEncoder,
      TelegramNotificationService notifications,
      AuditService auditService
  ) {
    this.accounts = accounts;
    this.consentService = consentService;
    this.passwordEncoder = passwordEncoder;
    this.notifications = notifications;
    this.auditService = auditService;
  }

  @Transactional
  public AuthenticatedAccount register(RegisterRequest request) {
    var role = normalizeAccountType(request.accountType());
    if (!request.password().equals(request.passwordConfirmation())) {
      throw new IllegalArgumentException("Пароли не совпадают.");
    }
    if (accounts.findByEmailIgnoreCase(request.email()).isPresent()) {
      throw new IllegalArgumentException("Аккаунт с таким email уже существует.");
    }
    var consent = consentService.require(
        request.consentGiven(),
        request.consentVersion(),
        request.privacyPolicyVersion(),
        role.equals("master") ? "master-registration" : "player-registration"
    );
    var status = role.equals("master") ? "pending_approval" : "active";
    var account = accounts.save(SiteAccountEntity.create(
        Ids.newId("acc"),
        request.displayName().trim(),
        request.email().trim().toLowerCase(Locale.ROOT),
        passwordEncoder.encode(request.password()),
        role,
        status,
        trimToNull(request.telegramUsername()),
        MasterAccessService.normalizeTelegramUsernamePublic(request.telegramUsername()),
        consent
    ));
    auditService.write(account.getId(), "account.registered", "SiteAccount", account.getId(), "{\"role\":\"" + role + "\",\"status\":\"" + status + "\"}");
    if (role.equals("master")) {
      notifications.notifyAdmins(String.join("\n",
          "Новая заявка на мастерский доступ",
          "",
          "Имя: " + account.getDisplayName(),
          "Email: " + account.getEmail(),
          account.getTelegramUsername() == null ? "" : "Telegram: " + account.getTelegramUsername(),
          "",
          "Подтвердить можно в кабинете Шляпника, раздел «Доступ»."
      ));
    }
    return AuthenticatedAccount.from(account);
  }

  @Transactional(readOnly = true)
  public AuthenticatedAccount login(LoginRequest request) {
    var account = accounts.findByEmailIgnoreCase(request.email())
        .orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль."));
    if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw new IllegalArgumentException("Неверный email или пароль.");
    }
    return AuthenticatedAccount.from(account);
  }

  private static String normalizeAccountType(String value) {
    var type = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    if (type.equals("player")) return "player";
    if (type.equals("master")) return "master";
    throw new IllegalArgumentException("Выберите тип аккаунта: игрок или мастер.");
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record AuthenticatedAccount(
      String id,
      String displayName,
      String email,
      String role,
      String status,
      String telegramUsername,
      Long telegramUserId,
      int sessionVersion
  ) {
    static AuthenticatedAccount from(SiteAccountEntity account) {
      return new AuthenticatedAccount(
          account.getId(),
          account.getDisplayName(),
          account.getEmail(),
          account.getRole(),
          account.getStatus(),
          account.getTelegramUsername(),
          account.getTelegramUserId(),
          account.getSessionVersion()
      );
    }
  }
}
