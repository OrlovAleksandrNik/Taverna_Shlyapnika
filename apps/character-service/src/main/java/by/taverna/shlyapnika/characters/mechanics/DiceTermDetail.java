package by.taverna.shlyapnika.characters.mechanics;

import by.taverna.shlyapnika.characters.rolls.RollMode;
import java.util.List;

public record DiceTermDetail(
    String expression,
    int count,
    int sides,
    RollMode mode,
    List<Integer> rolls,
    List<Integer> kept,
    int total
) {
}
