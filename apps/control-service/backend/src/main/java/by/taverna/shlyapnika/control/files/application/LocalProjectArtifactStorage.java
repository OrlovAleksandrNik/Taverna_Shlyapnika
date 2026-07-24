package by.taverna.shlyapnika.control.files.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class LocalProjectArtifactStorage implements ProjectArtifactStorage {
  private final Path root;

  public LocalProjectArtifactStorage(ControlProperties properties) {
    this.root = Path.of(properties.mediaStorageRoot()).resolve("project-artifacts").toAbsolutePath().normalize();
  }

  @Override
  public String describe() {
    return "local-project-artifact-storage";
  }

  @Override
  public String root() {
    return root.toString();
  }
}
