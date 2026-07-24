package by.taverna.shlyapnika.control.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_project_assignments")
public class ProjectAssignment {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "project_code", nullable = false)
  private String projectCode;
  @Column(name = "assignee_public_id", nullable = false)
  private String assigneePublicId;
  @Column(name = "role_hint", nullable = false)
  private String roleHint;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected ProjectAssignment() {
  }

  public ProjectAssignment(String projectCode, String assigneePublicId, String roleHint) {
    this.projectCode = projectCode;
    this.assigneePublicId = assigneePublicId;
    this.roleHint = roleHint;
  }

  public UUID getId() { return id; }
  public String getProjectCode() { return projectCode; }
  public String getAssigneePublicId() { return assigneePublicId; }
  public String getRoleHint() { return roleHint; }
  public Instant getCreatedAt() { return createdAt; }
}
