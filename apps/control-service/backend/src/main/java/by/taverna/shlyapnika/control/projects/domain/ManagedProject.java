package by.taverna.shlyapnika.control.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_managed_projects")
public class ManagedProject {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(nullable = false, unique = true)
  private String code;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private String kind;
  @Column(name = "detected_path", columnDefinition = "TEXT")
  private String detectedPath;
  @Column(columnDefinition = "TEXT")
  private String stack;
  @Column(nullable = false)
  private String status;
  @Column(name = "launch_mode", nullable = false)
  private String launchMode = "MOCK_ONLY";
  @Column(columnDefinition = "TEXT")
  private String notes;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ManagedProject() {
  }

  public ManagedProject(String code, String name, String kind, String detectedPath, String stack, String status, String notes) {
    this.code = code;
    this.name = name;
    this.kind = kind;
    this.detectedPath = detectedPath;
    this.stack = stack;
    this.status = status;
    this.notes = notes;
  }

  public String getCode() { return code; }
  public String getName() { return name; }
  public String getKind() { return kind; }
  public String getDetectedPath() { return detectedPath; }
  public String getStack() { return stack; }
  public String getStatus() { return status; }
  public String getLaunchMode() { return launchMode; }
  public String getNotes() { return notes; }
}
