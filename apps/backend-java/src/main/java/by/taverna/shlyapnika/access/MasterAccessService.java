package by.taverna.shlyapnika.access;

import by.taverna.shlyapnika.access.api.MasterAccessRequest;
import by.taverna.shlyapnika.access.api.MasterAccessResponse;
import by.taverna.shlyapnika.access.api.MasterLoginRequest;
import by.taverna.shlyapnika.access.domain.MasterAccessRequestEntity;
import by.taverna.shlyapnika.access.infrastructure.MasterAccessRequestRepository;
import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.config.TavernaProperties;
import by.taverna.shlyapnika.consent.ConsentService;
import by.taverna.shlyapnika.master.infrastructure.MasterRepository;
import by.taverna.shlyapnika.notification.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterAccessService {
  private static final Logger log = LoggerFactory.getLogger(MasterAccessService.class);

  private final MasterAccessRequestRepository requests;
  private final MasterRepository masters;
  private final ConsentService consentService;
  private final AuditService auditService;
  private final TelegramNotificationService notifications;
  private final TavernaProperties properties;

  public MasterAccessService(
      MasterAccessRequestRepository requests,
      MasterRepository masters,
      ConsentService consentService,
      AuditService auditService,
      TelegramNotificationService notifications,
      TavernaProperties properties
  ) {
    this.requests = requests;
    this.masters = masters;
    this.consentService = consentService;
    this.auditService = auditService;
    this.notifications = notifications;
    this.properties = properties;
  }

  @Transactional
  public MasterAccessResponse requestMasterAccess(MasterAccessRequest request) {
    var normalizedTelegram = normalizeTelegramUsername(request.telegramUsername());
    if (normalizedTelegram == null) throw new IllegalArgumentException("Укажите Telegram username, например @MisterHatter.");
    var codeRole = roleByAccessCode(request.accessCode());
    var requestedRole = codeRole == null ? "master" : codeRole;

    var existingApproved = requests.findFirstByNormalizedTelegramUsernameAndStatusOrderByCreatedAtDesc(normalizedTelegram, "approved");
    if (existingApproved.isPresent()) {
      return MasterAccessResponse.requested(
          existingApproved.get().getId(),
          "approved",
          "Мастерский доступ уже подтверждён. Можно войти через Telegram или e-mail."
      );
    }

    var existingPending = requests.findFirstByNormalizedTelegramUsernameAndStatusOrderByCreatedAtDesc(normalizedTelegram, "pending");
    if (existingPending.isPresent()) {
      // Код владельца позволяет подтвердить свой мастерский доступ без ручного решения в Telegram.
      if (codeRole != null) {
        var pending = existingPending.get();
        pending.approveAs(codeRole, null, "approved by owner access code");
        pending = requests.save(pending);
        auditService.write(null, "master.access.approved_by_code", "MasterAccessRequest", pending.getId(), null);
        notifications.notifyAdmins("Мастерский доступ подтверждён кодом владельца: " + pending.getDisplayName() + " (" + pending.getTelegramUsername() + ")");
        return MasterAccessResponse.approved(pending.getId(), accessCodeMessage(codeRole), pending.getDisplayName(), codeRole);
      }
      return MasterAccessResponse.requested(
          existingPending.get().getId(),
          "pending",
          "Заявка уже ожидает подтверждения Шляпника."
      );
    }

    var consent = consentService.require(
        request.consentGiven(),
        request.consentVersion(),
        request.privacyPolicyVersion(),
        "master-registration"
    );
    var entity = requests.save(MasterAccessRequestEntity.create(
        request.displayName(),
        request.email(),
        request.telegramUsername(),
        normalizedTelegram,
        requestedRole,
        consent
    ));
    // Новая заявка с верным кодом сразу становится мастерским доступом.
    if (codeRole != null) {
      entity.approveAs(codeRole, null, "approved by owner access code");
      entity = requests.save(entity);
      auditService.write(null, "master.access.approved_by_code", "MasterAccessRequest", entity.getId(), "{\"telegram\":\"" + normalizedTelegram + "\"}");
      notifications.notifyAdmins("Мастерский доступ подтверждён кодом владельца: " + entity.getDisplayName() + " (" + entity.getTelegramUsername() + ")");
      log.info("master access approved by owner code requestId={} telegram={}", entity.getId(), normalizedTelegram);
      return MasterAccessResponse.approved(entity.getId(), accessCodeMessage(codeRole), entity.getDisplayName(), codeRole);
    }
    auditService.write(null, "master.access.requested", "MasterAccessRequest", entity.getId(), "{\"telegram\":\"" + normalizedTelegram + "\"}");
    notifications.notifyAdmins(adminMessage(entity));
    log.info("master access requested requestId={} telegram={}", entity.getId(), normalizedTelegram);
    return MasterAccessResponse.requested(entity.getId(), entity.getStatus(), "Заявка отправлена Шляпнику на подтверждение.");
  }

  @Transactional(readOnly = true)
  public List<MasterAccessRequestDto> list(String status) {
    var normalizedStatus = status == null || status.isBlank() ? "pending" : status.trim();
    if (!List.of("pending", "approved", "rejected").contains(normalizedStatus)) {
      throw new IllegalArgumentException("Неизвестный статус заявки.");
    }
    return requests.findByStatusOrderByCreatedAtAsc(normalizedStatus).stream()
        .map(request -> new MasterAccessRequestDto(
            request.getId(),
            request.getDisplayName(),
            request.getEmail(),
            request.getTelegramUsername(),
            request.getRequestedRole(),
            request.getStatus(),
            request.getCreatedAt()
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public MasterAccessResponse login(MasterLoginRequest request) {
    var normalizedTelegram = normalizeTelegramUsername(request.telegramUsername());
    var email = trimToNull(request.email());
    if (normalizedTelegram == null && email == null) {
      throw new IllegalArgumentException("Укажите Telegram username или e-mail.");
    }

    var activeMaster = normalizedTelegram == null
        ? masters.findActiveMasters().stream().filter(master -> emailMatches(master.getContactUrl(), email)).findFirst()
        : masters.findActiveMasters().stream().filter(master -> master.hasTelegramUsername(normalizedTelegram)).findFirst();
    if (activeMaster.isPresent()) {
      var master = activeMaster.get();
      return MasterAccessResponse.login(true, "Добро пожаловать. Дневник открыт.", master.getDisplayName(), master.getRole());
    }

    var approvedRequest = normalizedTelegram == null
        ? requests.findFirstByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(email, "approved")
        : requests.findFirstByNormalizedTelegramUsernameAndStatusOrderByCreatedAtDesc(normalizedTelegram, "approved");
    if (approvedRequest.isPresent()) {
      return MasterAccessResponse.login(true, "Доступ подтверждён. Дневник открыт.", approvedRequest.get().getDisplayName(), approvedRequest.get().getRequestedRole());
    }

    return MasterAccessResponse.login(false, "Мастерский доступ пока не подтверждён.", null, "master");
  }

  @Transactional
  public MasterAccessRequestEntity approve(String requestId, Long adminTelegramId, String comment) {
    var request = requests.findById(requestId).orElseThrow(() -> new NotFoundException("Заявка на мастерский доступ не найдена."));
    request.approve(adminTelegramId, comment);
    request = requests.save(request);
    auditService.write(String.valueOf(adminTelegramId), "master.access.approved", "MasterAccessRequest", request.getId(), null);
    notifications.notifyAdmins("Мастерский доступ подтверждён: " + request.getDisplayName() + " (" + request.getTelegramUsername() + ")");
    return request;
  }

  @Transactional
  public MasterAccessRequestEntity reject(String requestId, Long adminTelegramId, String comment) {
    var request = requests.findById(requestId).orElseThrow(() -> new NotFoundException("Заявка на мастерский доступ не найдена."));
    request.reject(adminTelegramId, comment);
    request = requests.save(request);
    auditService.write(String.valueOf(adminTelegramId), "master.access.rejected", "MasterAccessRequest", request.getId(), null);
    notifications.notifyAdmins("Мастерский доступ отклонён: " + request.getDisplayName() + " (" + request.getTelegramUsername() + ")");
    return request;
  }

  private String adminMessage(MasterAccessRequestEntity request) {
    return String.join("\n",
        "Новая заявка на мастерский доступ",
        "",
        "Имя: " + request.getDisplayName(),
        "Telegram: " + request.getTelegramUsername(),
        "E-mail: " + request.getEmail(),
        "ID заявки: " + request.getId(),
        "",
        "Подтвердить сможет только администратор Таверны."
    );
  }

  private static String normalizeTelegramUsername(String value) {
    var trimmed = trimToNull(value);
    if (trimmed == null) return null;
    var normalized = trimmed
        .replace("https://t.me/", "")
        .replace("http://t.me/", "")
        .replace("https://telegram.me/", "")
        .replace("http://telegram.me/", "")
        .replace("@", "")
        .replace("/", "")
        .trim();
    return normalized.isBlank() ? null : normalized.toLowerCase();
  }

  private static boolean emailMatches(String contactUrl, String email) {
    return email != null && contactUrl != null && contactUrl.equalsIgnoreCase(email);
  }

  private String roleByAccessCode(String value) {
    var submittedCode = trimToNull(value);
    if (submittedCode == null) return null;
    var hatterCode = trimToNull(properties.hatterAccessCode());
    if (hatterCode != null && hatterCode.equals(submittedCode)) return "admin";
    var masterCode = trimToNull(properties.masterAccessCode());
    if (masterCode != null && masterCode.equals(submittedCode)) return "master";
    return null;
  }

  private static String accessCodeMessage(String role) {
    return "admin".equals(role)
        ? "Код Шляпника принят. Административный доступ открыт."
        : "Код принят. Мастерский доступ открыт.";
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record MasterAccessRequestDto(
      String id,
      String displayName,
      String email,
      String telegramUsername,
      String requestedRole,
      String status,
      Instant createdAt
  ) {
  }
}
