package by.taverna.shlyapnika.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {
  @Test
  void parsesRailwayPostgresUrl() {
    var parsed = DatabaseConfig.parseDatabaseUrl("postgresql://user:p%40ss@containers-us-west-1.railway.app:6543/railway?sslmode=require");

    assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://containers-us-west-1.railway.app:6543/railway?sslmode=require");
    assertThat(parsed.username()).isEqualTo("user");
    assertThat(parsed.password()).isEqualTo("p@ss");
  }

  @Test
  void keepsJdbcUrlUntouched() {
    var parsed = DatabaseConfig.parseDatabaseUrl("jdbc:postgresql://localhost:5432/taverna");

    assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/taverna");
    assertThat(parsed.username()).isBlank();
    assertThat(parsed.password()).isBlank();
  }

  @Test
  void convertsPrismaSchemaParamToCurrentSchema() {
    var parsed = DatabaseConfig.parseDatabaseUrl("postgresql://u:p@localhost/db?schema=public");

    assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/db?currentSchema=public");
  }
}
