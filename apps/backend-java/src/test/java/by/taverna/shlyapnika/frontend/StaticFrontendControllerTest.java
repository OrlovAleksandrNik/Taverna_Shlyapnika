package by.taverna.shlyapnika.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import by.taverna.shlyapnika.config.TavernaProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class StaticFrontendControllerTest {
  @TempDir
  private Path staticDir;

  @Test
  void returnsIndexWhenFrontendServingIsEnabled() throws Exception {
    Files.writeString(staticDir.resolve("index.html"), "<!doctype html><title>Таверна</title>", StandardCharsets.UTF_8);
    var controller = new StaticFrontendController(properties(true, staticDir));

    var response = controller.index();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void returnsRootHtmlPageWhenFrontendServingIsEnabled() throws Exception {
    Files.writeString(staticDir.resolve("dnd-beginners.html"), "<!doctype html><title>D&D</title>", StandardCharsets.UTF_8);
    var controller = new StaticFrontendController(properties(true, staticDir));

    var response = controller.htmlPage("dnd-beginners.html");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void returnsMasterCabinetSpaEntryWhenFrontendServingIsEnabled() throws Exception {
    var cabinetDir = Files.createDirectories(staticDir.resolve("master-cabinet"));
    Files.writeString(cabinetDir.resolve("index.html"), "<!doctype html><title>Кабинет мастера</title>", StandardCharsets.UTF_8);
    var controller = new StaticFrontendController(properties(true, staticDir));

    var response = controller.masterCabinet();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void returnsMasterCabinetNestedAssetWhenFrontendServingIsEnabled() throws Exception {
    var assetsDir = Files.createDirectories(staticDir.resolve("master-cabinet").resolve("assets"));
    Files.writeString(assetsDir.resolve("index.js"), "console.log('cabinet')", StandardCharsets.UTF_8);
    var controller = new StaticFrontendController(properties(true, staticDir));

    var response = controller.masterCabinetPath("/assets/index.js");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isNotEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getHeaders().getContentType().toString()).contains("javascript");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void returnsStylesheetContentTypeForMasterCabinetAsset() throws Exception {
    var assetsDir = Files.createDirectories(staticDir.resolve("master-cabinet").resolve("assets"));
    Files.writeString(assetsDir.resolve("index.css"), "body { color: white; }", StandardCharsets.UTF_8);
    var controller = new StaticFrontendController(properties(true, staticDir));

    var response = controller.masterCabinetPath("/assets/index.css");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("text/css"));
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
        "",
        "",
        "",
        true,
        serveFrontend,
        frontendDir.toString(),
        new TavernaProperties.Telegram("", "", "")
    );
  }
}
