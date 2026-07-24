package by.taverna.shlyapnika.control.audit.application;

import by.taverna.shlyapnika.control.audit.domain.AuditLog;
import by.taverna.shlyapnika.control.audit.infrastructure.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository logs;

  public AuditService(AuditLogRepository logs) {
    this.logs = logs;
  }

  public void record(String actorPublicId, String action, String entityType, String entityId, String details, String ipAddress) {
    logs.save(new AuditLog(actorPublicId, action, entityType, entityId, details, ipAddress));
  }
}
