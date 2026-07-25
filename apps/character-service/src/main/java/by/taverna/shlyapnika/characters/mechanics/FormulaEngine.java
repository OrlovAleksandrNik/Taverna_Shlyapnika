package by.taverna.shlyapnika.characters.mechanics;

import by.taverna.shlyapnika.characters.rolls.RollMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FormulaEngine {
  private static final Pattern VARIABLE = Pattern.compile("\\[([A-Za-z][A-Za-z0-9_]*)]");
  private static final Pattern DICE = Pattern.compile("(?i)(?<![A-Za-z0-9_])(\\d{0,3})d(\\d{1,4})(?![A-Za-z0-9_])");

  public FormulaEvaluation evaluate(String expression, Map<String, Integer> variables, RollMode mode, DiceRoller roller) {
    if (expression == null || expression.isBlank()) {
      throw new IllegalArgumentException("Formula is required.");
    }
    if (expression.length() > 240) {
      throw new IllegalArgumentException("Formula is too long.");
    }

    String withVariables = resolveVariables(expression, variables == null ? Map.of() : variables);
    List<DiceTermDetail> dice = new ArrayList<>();
    String resolved = rollDice(withVariables, mode == null ? RollMode.NORMAL : mode, roller, dice);
    double value = new MathParser(resolved).parse();
    return new FormulaEvaluation(expression, resolved, value, List.copyOf(dice));
  }

  private String resolveVariables(String expression, Map<String, Integer> variables) {
    Matcher matcher = VARIABLE.matcher(expression);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1).toUpperCase(Locale.ROOT);
      Integer value = variables.get(key);
      if (value == null) {
        throw new IllegalArgumentException("Missing formula variable [" + key + "].");
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private String rollDice(String expression, RollMode mode, DiceRoller roller, List<DiceTermDetail> details) {
    Matcher matcher = DICE.matcher(expression);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      int count = matcher.group(1).isBlank() ? 1 : Integer.parseInt(matcher.group(1));
      int sides = Integer.parseInt(matcher.group(2));
      if (count < 1 || count > 100) {
        throw new IllegalArgumentException("Dice count must be between 1 and 100.");
      }
      List<Integer> rolls = new ArrayList<>();
      List<Integer> kept = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        if (count == 1 && sides == 20 && mode != RollMode.NORMAL) {
          int first = roller.roll(sides);
          int second = roller.roll(sides);
          rolls.add(first);
          rolls.add(second);
          kept.add(mode == RollMode.ADVANTAGE ? Math.max(first, second) : Math.min(first, second));
        } else {
          int roll = roller.roll(sides);
          rolls.add(roll);
          kept.add(roll);
        }
      }
      int total = kept.stream().mapToInt(Integer::intValue).sum();
      details.add(new DiceTermDetail(matcher.group(), count, sides, mode, List.copyOf(rolls), List.copyOf(kept), total));
      matcher.appendReplacement(result, Matcher.quoteReplacement(Integer.toString(total)));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static final class MathParser {
    private final String input;
    private int pos = -1;
    private int ch;

    private MathParser(String input) {
      this.input = input;
      nextChar();
    }

    double parse() {
      double value = parseExpression();
      skipWhitespace();
      if (pos < input.length()) {
        throw new IllegalArgumentException("Unexpected formula token: " + (char) ch);
      }
      return value;
    }

    private void nextChar() {
      ch = (++pos < input.length()) ? input.charAt(pos) : -1;
    }

    private boolean eat(int expected) {
      skipWhitespace();
      if (ch == expected) {
        nextChar();
        return true;
      }
      return false;
    }

    private void skipWhitespace() {
      while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
        nextChar();
      }
    }

    private double parseExpression() {
      double value = parseTerm();
      while (true) {
        if (eat('+')) {
          value += parseTerm();
        } else if (eat('-')) {
          value -= parseTerm();
        } else {
          return value;
        }
      }
    }

    private double parseTerm() {
      double value = parseFactor();
      while (true) {
        if (eat('*')) {
          value *= parseFactor();
        } else if (eat('/')) {
          double divisor = parseFactor();
          if (divisor == 0) {
            throw new IllegalArgumentException("Division by zero in formula.");
          }
          value /= divisor;
        } else {
          return value;
        }
      }
    }

    private double parseFactor() {
      skipWhitespace();
      if (eat('+')) {
        return parseFactor();
      }
      if (eat('-')) {
        return -parseFactor();
      }
      if (eat('(')) {
        double value = parseExpression();
        if (!eat(')')) {
          throw new IllegalArgumentException("Missing closing parenthesis in formula.");
        }
        return value;
      }
      if (Character.isLetter(ch)) {
        return parseFunction();
      }
      return parseNumber();
    }

    private double parseFunction() {
      String name = readIdentifier().toLowerCase(Locale.ROOT);
      if (!eat('(')) {
        throw new IllegalArgumentException("Unknown formula token: " + name);
      }
      List<Double> args = new ArrayList<>();
      if (!eat(')')) {
        do {
          args.add(parseExpression());
        } while (eat(','));
        if (!eat(')')) {
          throw new IllegalArgumentException("Missing closing parenthesis in function " + name + ".");
        }
      }
      return switch (name) {
        case "floor" -> requireArg(name, args, 1) == 1 ? Math.floor(args.getFirst()) : 0;
        case "ceil" -> requireArg(name, args, 1) == 1 ? Math.ceil(args.getFirst()) : 0;
        case "max" -> requireMinArgs(name, args, 1).stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        case "min" -> requireMinArgs(name, args, 1).stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        default -> throw new IllegalArgumentException("Unknown formula function: " + name);
      };
    }

    private int requireArg(String name, List<Double> args, int count) {
      if (args.size() != count) {
        throw new IllegalArgumentException("Function " + name + " expects " + count + " argument(s).");
      }
      return count;
    }

    private List<Double> requireMinArgs(String name, List<Double> args, int count) {
      if (args.size() < count) {
        throw new IllegalArgumentException("Function " + name + " expects at least " + count + " argument(s).");
      }
      return args;
    }

    private String readIdentifier() {
      int start = pos;
      while (Character.isLetter(ch)) {
        nextChar();
      }
      return input.substring(start, pos);
    }

    private double parseNumber() {
      skipWhitespace();
      int start = pos;
      while ((ch >= '0' && ch <= '9') || ch == '.') {
        nextChar();
      }
      if (start == pos) {
        throw new IllegalArgumentException("Expected number in formula.");
      }
      return Double.parseDouble(input.substring(start, pos));
    }
  }
}
