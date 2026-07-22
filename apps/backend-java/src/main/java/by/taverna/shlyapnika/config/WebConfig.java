package by.taverna.shlyapnika.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final TavernaProperties properties;

  public WebConfig(TavernaProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
        .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
        .allowedHeaders("Content-Type", "Accept", "x-internal-token", "x-request-id")
        .maxAge(3600);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    var uploads = Path.of(properties.fileStorageDir()).toAbsolutePath().normalize();
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploads.toUri().toString())
        .setCachePeriod(30 * 24 * 60 * 60);

    if (properties.serveFrontend()) {
      var frontend = Path.of(properties.frontendStaticDir()).toAbsolutePath().normalize();
      // API routes are handled by controllers first; this handler is only the static site fallback.
      registry.addResourceHandler("/**")
          .addResourceLocations(frontend.toUri().toString())
          .setCachePeriod(60);
    }
  }
}
