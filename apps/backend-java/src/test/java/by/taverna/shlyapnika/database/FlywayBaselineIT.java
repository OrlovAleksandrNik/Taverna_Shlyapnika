package by.taverna.shlyapnika.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayBaselineIT {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("taverna_test")
      .withUsername("taverna")
      .withPassword("taverna");

  @Test
  void appliesBaselineSchemaToEmptyPostgres() {
    var flyway = Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .locations("classpath:db/migration")
        .load();

    var result = flyway.migrate();

    assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(1);
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
  }
}
