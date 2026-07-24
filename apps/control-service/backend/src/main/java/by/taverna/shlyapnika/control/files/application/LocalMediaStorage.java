package by.taverna.shlyapnika.control.files.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class LocalMediaStorage implements MediaStorage {
  private final Path root;

  public LocalMediaStorage(ControlProperties properties) {
    this.root = Path.of(properties.mediaStorageRoot()).toAbsolutePath().normalize();
  }

  @Override
  public String describe() {
    return "local-media-storage";
  }

  @Override
  public String root() {
    return root.toString();
  }
}
