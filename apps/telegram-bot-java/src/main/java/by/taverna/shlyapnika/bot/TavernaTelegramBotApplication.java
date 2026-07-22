package by.taverna.shlyapnika.bot;

import by.taverna.shlyapnika.bot.config.BotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BotProperties.class)
public class TavernaTelegramBotApplication {
  public static void main(String[] args) {
    SpringApplication.run(TavernaTelegramBotApplication.class, args);
  }
}
