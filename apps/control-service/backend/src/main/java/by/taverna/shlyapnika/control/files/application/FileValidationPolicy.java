package by.taverna.shlyapnika.control.files.application;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FileValidationPolicy {
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf", ".txt", ".md");
  private static final Set<String> BLOCKED_EXTENSIONS = Set.of(".exe", ".bat", ".cmd", ".ps1", ".msi", ".dll", ".scr", ".jar");

  public FileValidationResult validate(String fileName, String mimeType, long sizeBytes) {
    String normalized = fileName == null ? "" : fileName.trim().toLowerCase();
    String extension = normalized.contains(".") ? normalized.substring(normalized.lastIndexOf(".")) : "";
    if (normalized.isBlank() || normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
      return FileValidationResult.rejected("unsafe_name");
    }
    if (BLOCKED_EXTENSIONS.contains(extension)) {
      return FileValidationResult.rejected("executable_files_are_not_allowed");
    }
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      return FileValidationResult.rejected("extension_not_allowed");
    }
    if (sizeBytes <= 0 || sizeBytes > 25L * 1024L * 1024L) {
      return FileValidationResult.rejected("invalid_size");
    }
    if (mimeType == null || mimeType.isBlank() || mimeType.equals("application/octet-stream")) {
      return FileValidationResult.rejected("mime_type_required");
    }
    return new FileValidationResult(true, "accepted");
  }

  public Set<String> allowedExtensions() {
    return ALLOWED_EXTENSIONS;
  }

  public Set<String> blockedExtensions() {
    return BLOCKED_EXTENSIONS;
  }

  public record FileValidationResult(boolean accepted, String reason) {
    static FileValidationResult rejected(String reason) {
      return new FileValidationResult(false, reason);
    }
  }
}
