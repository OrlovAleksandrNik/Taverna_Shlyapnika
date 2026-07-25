package by.taverna.shlyapnika.characters.common;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class DatabaseConfig {
  @Bean
  HikariDataSource dataSource(Environment environment) {
    var configuredUrl = firstNonBlank(
        environment.getProperty("CHARACTER_DATABASE_JDBC_URL"),
        environment.getProperty("CHARACTER_DATABASE_URL"),
        environment.getProperty("SPRING_DATASOURCE_URL"),
        environment.getProperty("spring.datasource.url"),
        environment.getProperty("DATABASE_URL")
    );
    if (configuredUrl.isBlank() && environment.acceptsProfiles(Profiles.of("prod"))) {
      throw new IllegalStateException(
          "CHARACTER_DATABASE_URL, CHARACTER_DATABASE_JDBC_URL, SPRING_DATASOURCE_URL, or DATABASE_URL is required in production.");
    }

    var parsed = parseDatabaseUrl(firstNonBlank(configuredUrl, "jdbc:postgresql://localhost:5435/taverna_characters"));
    var datasource = new HikariDataSource();
    datasource.setJdbcUrl(parsed.jdbcUrl());
    datasource.setUsername(firstNonBlank(
        environment.getProperty("CHARACTER_DATABASE_USERNAME"),
        environment.getProperty("SPRING_DATASOURCE_USERNAME"),
        environment.getProperty("spring.datasource.username"),
        parsed.username(),
        "character"
    ));
    datasource.setPassword(firstNonBlank(
        environment.getProperty("CHARACTER_DATABASE_PASSWORD"),
        environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
        environment.getProperty("spring.datasource.password"),
        parsed.password(),
        "character"
    ));
    return datasource;
  }

  static ParsedDatabaseUrl parseDatabaseUrl(String rawUrl) {
    if (rawUrl.startsWith("jdbc:")) {
      return new ParsedDatabaseUrl(rawUrl, "", "");
    }
    if (!rawUrl.startsWith("postgresql://") && !rawUrl.startsWith("postgres://")) {
      return new ParsedDatabaseUrl(rawUrl, "", "");
    }

    var uri = URI.create(rawUrl.replace("postgres://", "postgresql://"));
    var userInfo = uri.getUserInfo();
    var username = "";
    var password = "";
    if (userInfo != null) {
      var parts = userInfo.split(":", 2);
      username = urlDecode(parts[0]);
      if (parts.length > 1) password = urlDecode(parts[1]);
    }

    var path = uri.getPath() == null || uri.getPath().isBlank() ? "/postgres" : uri.getPath();
    var query = jdbcQuery(uri.getRawQuery());
    return new ParsedDatabaseUrl("jdbc:postgresql://" + uri.getHost() + ":" + effectivePort(uri) + path + query,
        username, password);
  }

  private static String firstNonBlank(String... values) {
    for (var value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }

  private static int effectivePort(URI uri) {
    return uri.getPort() > 0 ? uri.getPort() : 5432;
  }

  private static String urlDecode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private static String jdbcQuery(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) return "";
    return "?" + rawQuery
        .replace("schema=", "currentSchema=")
        .replace("&schema=", "&currentSchema=");
  }

  record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
  }
}
