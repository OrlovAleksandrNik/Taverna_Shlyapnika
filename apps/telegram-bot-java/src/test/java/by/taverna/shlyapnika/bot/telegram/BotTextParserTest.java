package by.taverna.shlyapnika.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BotTextParserTest {
  @Test
  void normalizesTelegramContact() {
    assertThat(BotTextParser.contact("@hatter_master")).isEqualTo("https://t.me/hatter_master");
    assertThat(BotTextParser.contact("https://t.me/hatter_master")).isEqualTo("https://t.me/hatter_master");
  }

  @Test
  void rejectsInvalidTelegramContact() {
    assertThatThrownBy(() -> BotTextParser.contact("telegram me"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parsesPlayerRange() {
    var players = BotTextParser.players("3-5");

    assertThat(players.minPlayers()).isEqualTo(3);
    assertThat(players.maxPlayers()).isEqualTo(5);
  }

  @Test
  void parsesPriceWithDefaultCurrency() {
    var price = BotTextParser.price("35");

    assertThat(price.amount()).isEqualByComparingTo(new BigDecimal("35"));
    assertThat(price.currency()).isEqualTo("BYN");
  }

  @Test
  void parsesDurationInHours() {
    assertThat(BotTextParser.durationMinutes("3.5")).isEqualTo(210);
  }
}
