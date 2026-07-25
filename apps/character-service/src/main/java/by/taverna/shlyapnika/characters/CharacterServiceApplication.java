package by.taverna.shlyapnika.characters;

import by.taverna.shlyapnika.characters.common.CharacterServiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties(CharacterServiceProperties.class)
public class CharacterServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(CharacterServiceApplication.class, args);
  }
}
