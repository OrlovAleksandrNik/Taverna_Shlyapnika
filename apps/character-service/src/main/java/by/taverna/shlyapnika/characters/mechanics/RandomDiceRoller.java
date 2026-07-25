package by.taverna.shlyapnika.characters.mechanics;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class RandomDiceRoller implements DiceRoller {
  private final SecureRandom random = new SecureRandom();

  @Override
  public int roll(int sides) {
    if (sides < 2 || sides > 1000) {
      throw new IllegalArgumentException("Dice sides must be between 2 and 1000.");
    }
    return random.nextInt(sides) + 1;
  }
}
