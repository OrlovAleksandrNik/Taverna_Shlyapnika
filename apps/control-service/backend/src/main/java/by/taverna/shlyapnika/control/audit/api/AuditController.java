package by.taverna.shlyapnika.control.audit.api;

import by.taverna.shlyapnika.control.audit.domain.AuditLog;
import by.taverna.shlyapnika.control.audit.infrastructure.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AuditController {
  private final AuditLogRepository logs;

  public AuditController(AuditLogRepository logs) {
    this.logs = logs;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('audit.read')")
  Page<AuditLog> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
    if (q.isBlank()) {
      return logs.findAll(PageRequest.of(page, 30));
    }
    return logs.findByEntityTypeContainingIgnoreCaseOrActionContainingIgnoreCase(q, q, PageRequest.of(page, 30));
  }
}
