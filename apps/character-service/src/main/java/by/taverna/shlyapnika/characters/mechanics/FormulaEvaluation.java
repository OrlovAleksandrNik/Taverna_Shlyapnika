package by.taverna.shlyapnika.characters.mechanics;

import java.util.List;

public record FormulaEvaluation(
    String expression,
    String resolvedExpression,
    double value,
    List<DiceTermDetail> dice
) {
  public int intTotal() {
    return (int) Math.round(value);
  }
}
