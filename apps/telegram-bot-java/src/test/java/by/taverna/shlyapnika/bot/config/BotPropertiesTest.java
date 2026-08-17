package by.taverna.shlyapnika.bot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BotPropertiesTest {
  @Test
  void detectsConfiguredToken() {
    var properties = new BotProperties("token", "polling", "http://localhost:8080", "secret", 25, "Писарь Таверны", "Описание", "", 86400, "123,456");

    assertThat(properties.tokenConfigured()).isTrue();
    assertThat(properties.polling()).isTrue();
    assertThat(properties.safeDisplayName()).isEqualTo("Писарь Таверны");
    assertThat(properties.safeShortDescription()).isEqualTo("Описание");
    assertThat(properties.isHatter(123)).isTrue();
    assertThat(properties.isHatter(321)).isFalse();
  }

  @Test
  void disablesEmptyToken() {
    var properties = new BotProperties("", "webhook", "http://localhost:8080", "secret", 25, "", "", "", 86400, "");

    assertThat(properties.tokenConfigured()).isFalse();
    assertThat(properties.polling()).isFalse();
    assertThat(properties.safeDisplayName()).isEqualTo("Писарь Таверны");
    assertThat(properties.safeShortDescription()).contains("Создаёт афиши игр");
    assertThat(properties.isHatter(123)).isFalse();
  }
}
