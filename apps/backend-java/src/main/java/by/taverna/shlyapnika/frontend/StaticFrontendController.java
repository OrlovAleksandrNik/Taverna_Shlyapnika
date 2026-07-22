package by.taverna.shlyapnika.frontend;

import by.taverna.shlyapnika.config.TavernaProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticFrontendController {
  private final TavernaProperties properties;

  public StaticFrontendController(TavernaProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/")
  public ResponseEntity<Resource> index() {
    if (!properties.serveFrontend()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    var index = Path.of(properties.frontendStaticDir()).toAbsolutePath().normalize().resolve("index.html");
    if (!Files.isRegularFile(index)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    return ResponseEntity.ok(new PathResource(index));
  }
}
