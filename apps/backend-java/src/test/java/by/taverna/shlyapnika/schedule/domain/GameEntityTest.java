package by.taverna.shlyapnika.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import by.taverna.shlyapnika.master.domain.MasterEntity;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GameEntityTest {
  @Test
  void updatesEndTimeWhenStartOrDurationChanges() {
    var master = MasterEntity.create(123L, "hatter", "Шляпник", "https://t.me/hatter");
    var game = GameEntity.create(
        master,
        "Старая история",
        "Описание",
        "D&D 5e",
        "Новички",
        "12+",
        Instant.parse("2026-07-25T15:00:00Z"),
        180,
        3,
        5,
        new BigDecimal("35"),
        "BYN",
        "https://t.me/hatter",
        true
    );

    game.updateDateTimeStart(Instant.parse("2026-07-26T16:30:00Z"));
    game.updateDurationMinutes(240);

    assertThat(game.getDateTimeStart()).isEqualTo(Instant.parse("2026-07-26T16:30:00Z"));
    assertThat(game.getDurationMinutes()).isEqualTo(240);
    assertThat(game.getDateTimeEnd()).isEqualTo(Instant.parse("2026-07-26T20:30:00Z"));
  }

  @Test
  void keepsCurrentCurrencyWhenPriceCurrencyIsBlank() {
    var master = MasterEntity.create(123L, "hatter", "Шляпник", "https://t.me/hatter");
    var game = GameEntity.create(
        master,
        "История",
        "Описание",
        "D&D 5e",
        "Новички",
        "12+",
        Instant.parse("2026-07-25T15:00:00Z"),
        null,
        3,
        5,
        new BigDecimal("35"),
        "BYN",
        "https://t.me/hatter",
        true
    );

    game.updatePrice(new BigDecimal("45"), " ");

    assertThat(game.getPrice()).isEqualByComparingTo(new BigDecimal("45"));
    assertThat(game.getCurrency()).isEqualTo("BYN");
  }
}
