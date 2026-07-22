package by.taverna.shlyapnika.application;

import by.taverna.shlyapnika.application.api.ServiceRequestRequest;
import by.taverna.shlyapnika.application.domain.ServiceRequestEntity;
import by.taverna.shlyapnika.application.infrastructure.ServiceRequestRepository;
import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.consent.ConsentService;
import by.taverna.shlyapnika.notification.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestService {
  private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

  private final ServiceRequestRepository repository;
  private final ConsentService consentService;
  private final AuditService auditService;
  private final TelegramNotificationService notifications;

  public ServiceRequestService(
      ServiceRequestRepository repository,
      ConsentService consentService,
      AuditService auditService,
      TelegramNotificationService notifications
  ) {
    this.repository = repository;
    this.consentService = consentService;
    this.auditService = auditService;
    this.notifications = notifications;
  }

  @Transactional
  public ServiceRequestEntity create(ServiceRequestRequest request) {
    var consent = consentService.require(
        request.consentGiven(),
        request.consentVersion(),
        request.privacyPolicyVersion(),
        "service-request"
    );
    var entity = repository.save(ServiceRequestEntity.create(
        request.name(),
        request.contact(),
        request.service(),
        request.desiredDate(),
        request.participants(),
        request.city(),
        request.comment(),
        consent
    ));
    auditService.write(null, "service.request", "ServiceRequest", entity.getId(), "{\"service\":\"" + entity.getService() + "\"}");
    notifications.notifyAdmins(adminMessage(request));
    log.info("service request saved requestId={} service={}", entity.getId(), entity.getService());
    return entity;
  }

  private String adminMessage(ServiceRequestRequest request) {
    return String.join("\n",
        "Новая заявка на услугу",
        "",
        "Услуга: " + request.service(),
        "Имя: " + request.name(),
        "Контакт: " + request.contact(),
        request.desiredDate() == null || request.desiredDate().isBlank() ? "" : "Дата: " + request.desiredDate(),
        request.participants() == null ? "" : "Участников: " + request.participants(),
        request.city() == null || request.city().isBlank() ? "" : "Город: " + request.city(),
        request.comment() == null || request.comment().isBlank() ? "" : "Комментарий: " + request.comment()
    ).trim();
  }
}
