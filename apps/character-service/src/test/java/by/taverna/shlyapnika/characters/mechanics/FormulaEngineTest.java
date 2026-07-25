package by.taverna.shlyapnika.characters.mechanics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import by.taverna.shlyapnika.characters.rolls.RollMode;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormulaEngineTest {
  private final FormulaEngine engine = new FormulaEngine();

  @Test
  void evaluatesVariablesMathAndFunctions() {
    FormulaEvaluation result = engine.evaluate("floor(([STR] - 10) / 2) + [PROF]",
        Map.of("STR", 18, "PROF", 3), RollMode.NORMAL, new FixedDiceRoller());

    assertThat(result.value()).isEqualTo(7);
    assertThat(result.resolvedExpression()).isEqualTo("floor((18 - 10) / 2) + 3");
    assertThat(result.dice()).isEmpty();
  }

  @Test
  void rollsDiceInsideFormula() {
    FormulaEvaluation result = engine.evaluate("1d6 + [DEX]", Map.of("DEX", 4),
        RollMode.NORMAL, new FixedDiceRoller(5));

    assertThat(result.intTotal()).isEqualTo(9);
    assertThat(result.resolvedExpression()).isEqualTo("5 + 4");
    assertThat(result.dice()).hasSize(1);
    assertThat(result.dice().getFirst().rolls()).containsExactly(5);
  }

  @Test
  void keepsHigherD20ForAdvantage() {
    FormulaEvaluation result = engine.evaluate("d20 + 4", Map.of(),
        RollMode.ADVANTAGE, new FixedDiceRoller(6, 17));

    assertThat(result.intTotal()).isEqualTo(21);
    assertThat(result.dice().getFirst().rolls()).containsExactly(6, 17);
    assertThat(result.dice().getFirst().kept()).containsExactly(17);
  }

  @Test
  void keepsLowerD20ForDisadvantage() {
    FormulaEvaluation result = engine.evaluate("1d20 + 4", Map.of(),
        RollMode.DISADVANTAGE, new FixedDiceRoller(6, 17));

    assertThat(result.intTotal()).isEqualTo(10);
    assertThat(result.dice().getFirst().kept()).containsExactly(6);
  }

  @Test
  void rejectsUnknownVariables() {
    assertThatThrownBy(() -> engine.evaluate("[CHA] + 2", Map.of(), RollMode.NORMAL, new FixedDiceRoller()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("[CHA]");
  }

  private static final class FixedDiceRoller implements DiceRoller {
    private final ArrayDeque<Integer> rolls;

    private FixedDiceRoller(Integer... rolls) {
      this.rolls = new ArrayDeque<>(List.of(rolls));
    }

    @Override
    public int roll(int sides) {
      return rolls.isEmpty() ? 1 : rolls.removeFirst();
    }
  }
}
