package by.taverna.shlyapnika.bot.telegram;

import java.math.BigDecimal;

public class GameDraft {
  private String displayName;
  private String contactUrl;
  private String title;
  private String date;
  private String time;
  private Integer durationMinutes;
  private String description;
  private Integer minPlayers;
  private Integer maxPlayers;
  private BigDecimal price;
  private String currency = "BYN";
  private String gameSystem;
  private String experienceLevel;
  private String ageRating;
  private String contactOverride;

  public String displayName() { return displayName; }
  public void displayName(String value) { displayName = value; }
  public String contactUrl() { return contactUrl; }
  public void contactUrl(String value) { contactUrl = value; }
  public String title() { return title; }
  public void title(String value) { title = value; }
  public String date() { return date; }
  public void date(String value) { date = value; }
  public String time() { return time; }
  public void time(String value) { time = value; }
  public Integer durationMinutes() { return durationMinutes; }
  public void durationMinutes(Integer value) { durationMinutes = value; }
  public String description() { return description; }
  public void description(String value) { description = value; }
  public Integer minPlayers() { return minPlayers; }
  public void minPlayers(Integer value) { minPlayers = value; }
  public Integer maxPlayers() { return maxPlayers; }
  public void maxPlayers(Integer value) { maxPlayers = value; }
  public BigDecimal price() { return price; }
  public void price(BigDecimal value) { price = value; }
  public String currency() { return currency; }
  public void currency(String value) { currency = value; }
  public String gameSystem() { return gameSystem; }
  public void gameSystem(String value) { gameSystem = value; }
  public String experienceLevel() { return experienceLevel; }
  public void experienceLevel(String value) { experienceLevel = value; }
  public String ageRating() { return ageRating; }
  public void ageRating(String value) { ageRating = value; }
  public String contactOverride() { return contactOverride; }
  public void contactOverride(String value) { contactOverride = value; }
}
