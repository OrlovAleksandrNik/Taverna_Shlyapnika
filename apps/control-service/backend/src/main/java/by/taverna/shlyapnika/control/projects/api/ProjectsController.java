package by.taverna.shlyapnika.control.projects.api;

import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.projects.domain.ManagedProject;
import by.taverna.shlyapnika.control.projects.infrastructure.ManagedProjectRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projects")
public class ProjectsController {
  private final ManagedProjectRepository projects;
  private final AuditService audit;

  public ProjectsController(ManagedProjectRepository projects, AuditService audit) {
    this.projects = projects;
    this.audit = audit;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('projects.read')")
  List<Map<String, Object>> list() {
    return projects.findAll().stream().map(this::toResponse).toList();
  }

  @PostMapping("/{code}/launch")
  @PreAuthorize("hasAuthority('projects.launch')")
  Map<String, Object> mockLaunch(@PathVariable String code, Authentication authentication, HttpServletRequest request) {
    ManagedProject project = projects.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Проект не найден."));
    audit.record(authentication.getName(), "projects.mock_launch", "ManagedProject", code, "Desktop Agent disabled; no executable started", request.getRemoteAddr());
    return Map.of(
        "code", project.getCode(),
        "status", "mock-launch-recorded",
        "desktopAgentRequired", true,
        "startedAt", Instant.now(),
        "message", "Реальный запуск отключён до появления Desktop Agent и allowlist-политики.");
  }

  private Map<String, Object> toResponse(ManagedProject project) {
    return Map.of(
        "code", project.getCode(),
        "name", project.getName(),
        "kind", project.getKind(),
        "detectedPath", project.getDetectedPath(),
        "stack", project.getStack(),
        "status", project.getStatus(),
        "launchMode", project.getLaunchMode(),
        "notes", project.getNotes());
  }
}
