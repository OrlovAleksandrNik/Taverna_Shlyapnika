package by.taverna.shlyapnika.audit;

import by.taverna.shlyapnika.audit.domain.AuditLogEntity;
import by.taverna.shlyapnika.audit.infrastructure.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private static final Logger log = LoggerFactory.getLogger(AuditService.class);
  private final AuditLogRepository repository;

  public AuditService(AuditLogRepository repository) {
    this.repository = repository;
  }

  public void write(String userId, String action, String entityType, String entityId, String detailsJson) {
    try {
      repository.save(AuditLogEntity.of(userId, action, entityType, entityId, detailsJson));
    } catch (Exception error) {
      log.warn("audit log write failed for action={} entityType={} entityId={}", action, entityType, entityId, error);
    }
  }
}
