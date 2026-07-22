package by.taverna.shlyapnika.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import by.taverna.shlyapnika.config.TavernaProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

class StaticFrontendControllerTest {
  @TempDir
  private Path staticDir;

  @Test
  void returnsIndexWhenFrontendServingIsEnabled() throws Exception {
    Files.writeString(staticDir.resolve("index.html"), "<!doctype html><title>Таверна</title>", StandardCharsets.UTF_8);
    var controller = new StaticFrontendController(properties(true, staticDir));

    var response = controller.index();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void returnsNotFoundWhenFrontendServingIsDisabled() {
    var controller = new StaticFrontendController(properties(false, staticDir));

    var response = controller.index();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private static TavernaProperties properties(boolean serveFrontend, Path frontendDir) {
    return new TavernaProperties(
        "http://localhost:8080",
        "/uploads",
        "uploads",
        "Europe/Minsk",
        "http://localhost:4177",
        "test-internal-token",
        true,
        serveFrontend,
        frontendDir.toString(),
        new TavernaProperties.Telegram("", "")
    );
  }
}
