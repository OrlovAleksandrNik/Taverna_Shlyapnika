package by.taverna.shlyapnika.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import by.taverna.shlyapnika.config.TavernaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthControllerTest {
  private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
  private final HealthController controller = new HealthController(jdbcTemplate, properties());

  @Test
  void healthReturnsOkEvenWhenDatabaseIsUnavailable() {
    when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenThrow(new IllegalStateException("database down"));

    var body = controller.health();

    assertThat(body.get("ok")).isEqualTo(true);
    assertThat(body.get("database")).isEqualTo("error");
    assertThat(body.get("backend")).isEqualTo("ok");
  }

  @Test
  void readyReturnsServiceUnavailableWhenDatabaseIsUnavailable() {
    when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenThrow(new IllegalStateException("database down"));

    var response = controller.ready();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("ok")).isEqualTo(false);
    assertThat(response.getBody().get("database")).isEqualTo("error");
  }

  @Test
  void readyReturnsOkWhenDatabaseResponds() {
    when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(1);

    var response = controller.ready();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("ok")).isEqualTo(true);
    assertThat(response.getBody().get("database")).isEqualTo("ok");
  }

  private static TavernaProperties properties() {
    return new TavernaProperties(
        "http://localhost:8080",
        "/uploads",
        "uploads",
        "Europe/Minsk",
        "http://localhost:4177",
        "test-internal-token",
        true,
        false,
        "static-site",
        new TavernaProperties.Telegram("", "")
    );
  }
}
