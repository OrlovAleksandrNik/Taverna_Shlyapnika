package by.taverna.shlyapnika.control;

import by.taverna.shlyapnika.control.config.ControlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ControlProperties.class)
public class ControlServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(ControlServiceApplication.class, args);
  }
}
