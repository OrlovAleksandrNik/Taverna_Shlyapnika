package by.taverna.shlyapnika.control.projects.api;

import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.projects.application.DesktopAgentLauncher;
import by.taverna.shlyapnika.control.projects.domain.ManagedProject;
import by.taverna.shlyapnika.control.projects.domain.ProjectAssignment;
import by.taverna.shlyapnika.control.projects.infrastructure.ManagedProjectRepository;
import by.taverna.shlyapnika.control.projects.infrastructure.ProjectAssignmentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projects")
public class ProjectsController {
  private final ManagedProjectRepository projects;
  private final ProjectAssignmentRepository assignments;
  private final AuditService audit;
  private final DesktopAgentLauncher desktopAgentLauncher;

  public ProjectsController(ManagedProjectRepository projects, ProjectAssignmentRepository assignments, AuditService audit, DesktopAgentLauncher desktopAgentLauncher) {
    this.projects = projects;
    this.assignments = assignments;
    this.audit = audit;
    this.desktopAgentLauncher = desktopAgentLauncher;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('projects.read')")
  List<Map<String, Object>> list() {
    return projects.findAll().stream().map(this::toResponse).toList();
  }

  @PostMapping("/{code}/launch")
  @PreAuthorize("hasAuthority('projects.launch')")
  DesktopAgentLauncher.LaunchResult launch(@PathVariable String code, Authentication authentication, HttpServletRequest request) {
    ManagedProject project = projects.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Project not found."));
    DesktopAgentLauncher.LaunchResult result = desktopAgentLauncher.launch(project);
    audit.record(authentication.getName(), "projects.launch", "ManagedProject", code, result.status() + "; " + result.message(), request.getRemoteAddr());
    return result;
  }

  @GetMapping("/{code}/assignments")
  @PreAuthorize("hasAuthority('projects.read')")
  List<ProjectAssignment> assignments(@PathVariable String code) {
    return assignments.findByProjectCodeOrderByCreatedAtDesc(code);
  }

  @PostMapping("/{code}/assignments")
  @PreAuthorize("hasAuthority('projects.configure')")
  ProjectAssignment assign(@PathVariable String code, @Valid @RequestBody AssignmentRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    projects.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Project not found."));
    ProjectAssignment assignment = assignments.save(new ProjectAssignment(code, request.assigneePublicId(), request.roleHint()));
    audit.record(authentication.getName(), "projects.assign", "ManagedProject", code, "assignee=" + request.assigneePublicId(), servletRequest.getRemoteAddr());
    return assignment;
  }

  @DeleteMapping("/assignments/{id}")
  @PreAuthorize("hasAuthority('projects.configure')")
  void revokeAssignment(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    ProjectAssignment assignment = assignments.findById(id).orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
    assignments.delete(assignment);
    audit.record(authentication.getName(), "projects.revoke_access", "ProjectAssignment", id.toString(), "project=" + assignment.getProjectCode(), servletRequest.getRemoteAddr());
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

  public record AssignmentRequest(@NotBlank String assigneePublicId, @NotBlank String roleHint) {
  }
}
