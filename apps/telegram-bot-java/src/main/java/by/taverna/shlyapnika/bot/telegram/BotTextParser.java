package by.taverna.shlyapnika.bot.telegram;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public final class BotTextParser {
  private static final Pattern CONTACT = Pattern.compile("^(@[A-Za-z0-9_]{5,32}|https://t\\.me/[A-Za-z0-9_]{5,32})$");
  private static final Pattern PLAYERS = Pattern.compile("^(\\d{1,2})(?:\\s*[-–]\\s*(\\d{1,2}))?$");
  private static final Pattern PRICE = Pattern.compile("^(\\d+(?:[\\.,]\\d{1,2})?)\\s*([A-Za-zА-Яа-я]{3})?$");

  private BotTextParser() {
  }

  public static String clean(String value) {
    if (value == null) return "";
    return value.replace("<", "").replace(">", "").replaceAll("\\s+", " ").trim();
  }

  public static String title(String value) {
    var text = clean(value);
    if (text.length() < 3 || text.length() > 100) throw new IllegalArgumentException("Название должно быть от 3 до 100 символов.");
    return text;
  }

  public static String description(String value) {
    var text = clean(value);
    if (text.length() < 20 || text.length() > 1000) throw new IllegalArgumentException("Описание должно быть от 20 до 1000 символов.");
    return text;
  }

  public static String shortText(String value, String label, int min, int max) {
    var text = clean(value);
    if (text.length() < min || text.length() > max) throw new IllegalArgumentException(label + " должно быть от " + min + " до " + max + " символов.");
    return text;
  }

  public static String story(String value, String label, int min, int max) {
    var text = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').trim();
    if (text.length() < min || text.length() > max) throw new IllegalArgumentException(label + " должен быть от " + min + " до " + max + " символов.");
    return text;
  }

  public static String contact(String value) {
    var text = clean(value);
    if (!CONTACT.matcher(text).matches()) throw new IllegalArgumentException("Укажите контакт в формате @username или https://t.me/username.");
    return text.startsWith("@") ? "https://t.me/" + text.substring(1) : text;
  }

  public static void validateDateTime(String date, String time) {
    try {
      LocalDate.parse(date);
      LocalTime.parse(time);
    } catch (Exception error) {
      throw new IllegalArgumentException("Введите дату и время в формате YYYY-MM-DD и HH:MM.");
    }
  }

  public static int durationMinutes(String value) {
    var hours = Double.parseDouble(clean(value).replace(",", "."));
    if (!Double.isFinite(hours) || hours <= 0 || hours > 12) throw new IllegalArgumentException("Введите продолжительность в часах, например 3.5.");
    return (int) Math.round(hours * 60);
  }

  public static Players players(String value) {
    var match = PLAYERS.matcher(clean(value));
    if (!match.matches()) throw new IllegalArgumentException("Введите число от 1 до 20 или диапазон, например 3-5.");
    var min = Integer.parseInt(match.group(1));
    var max = match.group(2) == null ? min : Integer.parseInt(match.group(2));
    if (min < 1 || max > 20 || min > max) throw new IllegalArgumentException("Введите число от 1 до 20 или диапазон, например 3-5.");
    return new Players(min, max);
  }

  public static Price price(String value) {
    var match = PRICE.matcher(clean(value).replace(",", "."));
    if (!match.matches()) throw new IllegalArgumentException("Пример стоимости: 35 BYN. Для бесплатной игры: 0 BYN.");
    var amount = new BigDecimal(match.group(1));
    if (amount.signum() < 0) throw new IllegalArgumentException("Стоимость не может быть отрицательной.");
    return new Price(amount, match.group(2) == null ? "BYN" : match.group(2).toUpperCase());
  }

  public record Players(int minPlayers, int maxPlayers) {
  }

  public record Price(BigDecimal amount, String currency) {
  }
}
