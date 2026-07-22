package by.taverna.shlyapnika.audit.infrastructure;

import by.taverna.shlyapnika.audit.domain.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
}
