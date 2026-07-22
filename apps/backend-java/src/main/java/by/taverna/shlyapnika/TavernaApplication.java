package by.taverna.shlyapnika;

import by.taverna.shlyapnika.config.TavernaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(TavernaProperties.class)
public class TavernaApplication {
  public static void main(String[] args) {
    SpringApplication.run(TavernaApplication.class, args);
  }
}
