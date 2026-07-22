package by.taverna.shlyapnika.bot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BotPropertiesTest {
  @Test
  void detectsConfiguredToken() {
    var properties = new BotProperties("token", "polling", "http://localhost:8080", "secret", 25);

    assertThat(properties.tokenConfigured()).isTrue();
    assertThat(properties.polling()).isTrue();
  }

  @Test
  void disablesEmptyToken() {
    var properties = new BotProperties("", "webhook", "http://localhost:8080", "secret", 25);

    assertThat(properties.tokenConfigured()).isFalse();
    assertThat(properties.polling()).isFalse();
  }
}
