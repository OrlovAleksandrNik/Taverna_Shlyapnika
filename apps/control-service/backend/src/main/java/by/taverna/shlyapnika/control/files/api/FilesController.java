package by.taverna.shlyapnika.control.files.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import by.taverna.shlyapnika.control.files.application.FileValidationPolicy;
import by.taverna.shlyapnika.control.files.application.MediaStorage;
import by.taverna.shlyapnika.control.files.application.ProjectArtifactStorage;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/files")
@PreAuthorize("hasAuthority('files.manage')")
public class FilesController extends SectionStubController {
  private final MediaStorage mediaStorage;
  private final ProjectArtifactStorage projectArtifactStorage;
  private final FileValidationPolicy validationPolicy;

  public FilesController(MediaStorage mediaStorage, ProjectArtifactStorage projectArtifactStorage, FileValidationPolicy validationPolicy) {
    super("files");
    this.mediaStorage = mediaStorage;
    this.projectArtifactStorage = projectArtifactStorage;
    this.validationPolicy = validationPolicy;
  }

  @GetMapping("/storage")
  public Map<String, Object> storage() {
    return Map.of(
        "media", Map.of("adapter", mediaStorage.describe(), "root", mediaStorage.root()),
        "projectArtifacts", Map.of("adapter", projectArtifactStorage.describe(), "root", projectArtifactStorage.root()),
        "futureAdapters", java.util.List.of("s3-compatible-media-storage", "s3-compatible-backup-storage", "s3-compatible-project-artifact-storage"));
  }

  @GetMapping("/validate")
  public FileValidationPolicy.FileValidationResult validate(
      @RequestParam String fileName,
      @RequestParam String mimeType,
      @RequestParam long sizeBytes) {
    return validationPolicy.validate(fileName, mimeType, sizeBytes);
  }
}
