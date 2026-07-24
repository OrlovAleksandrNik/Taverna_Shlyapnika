package by.taverna.shlyapnika.control.games;

import by.taverna.shlyapnika.control.games.domain.ControlGame;
import by.taverna.shlyapnika.control.games.domain.GameStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameEntityTest {
  @Test
  void publishAndSoftDeleteAreExplicitStateTransitions() {
    ControlGame game = new ControlGame("Test", "Description", "D&D 5e", "beginners", Instant.now(), 180, 2, 5, BigDecimal.TEN);

    game.publish();
    assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);

    game.softDelete();
    assertThat(game.getStatus()).isEqualTo(GameStatus.ARCHIVED);
  }
}
