package by.taverna.shlyapnika.characters.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {
  @Test
  void parsesRailwayStyleDatabaseUrl() {
    var parsed = DatabaseConfig.parseDatabaseUrl("postgresql://demo:pa%24%24@containers-us-west-1.railway.app:6543/railway");

    assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://containers-us-west-1.railway.app:6543/railway");
    assertThat(parsed.username()).isEqualTo("demo");
    assertThat(parsed.password()).isEqualTo("pa$$");
  }

  @Test
  void preservesJdbcUrl() {
    var parsed = DatabaseConfig.parseDatabaseUrl("jdbc:postgresql://localhost:5435/taverna_characters");

    assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5435/taverna_characters");
    assertThat(parsed.username()).isBlank();
    assertThat(parsed.password()).isBlank();
  }
}
