package by.taverna.shlyapnika.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI tavernaOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Таверна Шляпника API")
            .version("0.1.0")
            .description("Java backend API, совместимый с текущим frontend."));
  }

  @Bean
  GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("Public API")
        .pathsToMatch("/api/games/**", "/api/schedule", "/api/service-requests", "/api/game-signups", "/api/gallery/**", "/api/rating", "/api/masters")
        .build();
  }

  @Bean
  GroupedOpenApi internalApi() {
    return GroupedOpenApi.builder()
        .group("Internal API")
        .pathsToMatch("/api/internal/**")
        .build();
  }

  @Bean
  GroupedOpenApi healthApi() {
    return GroupedOpenApi.builder()
        .group("Health API")
        .pathsToMatch("/health", "/ready", "/actuator/health/**")
        .build();
  }
}
