package by.taverna.shlyapnika.frontend;

import by.taverna.shlyapnika.config.TavernaProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticFrontendController {
  private final TavernaProperties properties;

  public StaticFrontendController(TavernaProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/")
  public ResponseEntity<Resource> index() {
    return frontendPage("index.html");
  }

  @GetMapping("/{page:[a-z0-9-]+\\.html}")
  public ResponseEntity<Resource> htmlPage(@PathVariable String page) {
    return frontendPage(page);
  }

  @GetMapping({"/master-cabinet", "/master-cabinet/", "/master-cabinet/{path:[a-z0-9-]+}"})
  public ResponseEntity<Resource> masterCabinet() {
    // Кабинет мастера является вложенным SPA, поэтому все его клиентские маршруты ведут на index.html.
    return frontendPage("master-cabinet/index.html");
  }

  private ResponseEntity<Resource> frontendPage(String page) {
    if (!properties.serveFrontend()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    var frontendRoot = Path.of(properties.frontendStaticDir()).toAbsolutePath().normalize();
    var file = frontendRoot.resolve(page).normalize();
    if (!file.startsWith(frontendRoot) || !Files.isRegularFile(file)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    return ResponseEntity.ok(new PathResource(file));
  }
}
