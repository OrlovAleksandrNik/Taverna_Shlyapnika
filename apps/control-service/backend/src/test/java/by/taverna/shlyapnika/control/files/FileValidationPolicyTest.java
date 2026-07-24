package by.taverna.shlyapnika.control.files;

import by.taverna.shlyapnika.control.files.application.FileValidationPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileValidationPolicyTest {
  private final FileValidationPolicy policy = new FileValidationPolicy();

  @Test
  void rejectsExecutableUpload() {
    assertThat(policy.validate("tool.exe", "application/octet-stream", 1024).accepted()).isFalse();
  }

  @Test
  void acceptsKnownImageUpload() {
    assertThat(policy.validate("photo.webp", "image/webp", 1024).accepted()).isTrue();
  }
}
