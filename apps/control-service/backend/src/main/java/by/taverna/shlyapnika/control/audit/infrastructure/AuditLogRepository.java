package by.taverna.shlyapnika.control.audit.infrastructure;

import by.taverna.shlyapnika.control.audit.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  Page<AuditLog> findByEntityTypeContainingIgnoreCaseOrActionContainingIgnoreCase(String entityType, String action, Pageable pageable);
}
