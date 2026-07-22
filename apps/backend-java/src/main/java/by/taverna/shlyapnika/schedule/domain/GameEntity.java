package by.taverna.shlyapnika.schedule.domain;

import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.master.domain.MasterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "\"Game\"")
public class GameEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"masterId\"", nullable = false)
  private MasterEntity master;

  @Column(name = "\"title\"", nullable = false)
  private String title;

  @Column(name = "\"description\"", nullable = false)
  private String description;

  @Column(name = "\"gameSystem\"", nullable = false)
  private String gameSystem;

  @Column(name = "\"experienceLevel\"", nullable = false)
  private String experienceLevel;

  @Column(name = "\"ageRating\"", nullable = false)
  private String ageRating;

  @Column(name = "\"dateTimeStart\"", nullable = false)
  private Instant dateTimeStart;

  @Column(name = "\"durationMinutes\"")
  private Integer durationMinutes;

  @Column(name = "\"dateTimeEnd\"")
  private Instant dateTimeEnd;

  @Column(name = "\"minPlayers\"", nullable = false)
  private Integer minPlayers;

  @Column(name = "\"maxPlayers\"", nullable = false)
  private Integer maxPlayers;

  @Column(name = "\"price\"", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "\"currency\"", nullable = false)
  private String currency = "BYN";

  @Column(name = "\"imageUrl\"")
  private String imageUrl;

  @Column(name = "\"contactUrl\"", nullable = false)
  private String contactUrl;

  @Column(name = "\"status\"", nullable = false)
  @ColumnTransformer(write = "?::\"GameStatus\"")
  private String status = "draft";

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  @Column(name = "\"publishedAt\"")
  private Instant publishedAt;

  @Column(name = "\"completedAt\"")
  private Instant completedAt;

  @Column(name = "\"cancelledAt\"")
  private Instant cancelledAt;

  public static GameEntity create(
      MasterEntity master,
      String title,
      String description,
      String gameSystem,
      String experienceLevel,
      String ageRating,
      Instant dateTimeStart,
      Integer durationMinutes,
      Integer minPlayers,
      Integer maxPlayers,
      BigDecimal price,
      String currency,
      String contactUrl,
      boolean autoPublish
  ) {
    var game = new GameEntity();
    game.master = master;
    game.title = title;
    game.description = description;
    game.gameSystem = gameSystem;
    game.experienceLevel = experienceLevel;
    game.ageRating = ageRating;
    game.dateTimeStart = dateTimeStart;
    game.durationMinutes = durationMinutes;
    game.dateTimeEnd = durationMinutes == null ? null : dateTimeStart.plusSeconds(durationMinutes.longValue() * 60);
    game.minPlayers = minPlayers;
    game.maxPlayers = maxPlayers;
    game.price = price;
    game.currency = currency == null || currency.isBlank() ? "BYN" : currency;
    game.contactUrl = contactUrl;
    game.status = autoPublish ? "published" : "pending";
    game.publishedAt = autoPublish ? Instant.now() : null;
    return game;
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("gm");
    var now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public String getId() { return id; }
  public MasterEntity getMaster() { return master; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public String getGameSystem() { return gameSystem; }
  public String getExperienceLevel() { return experienceLevel; }
  public String getAgeRating() { return ageRating; }
  public Instant getDateTimeStart() { return dateTimeStart; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public Instant getDateTimeEnd() { return dateTimeEnd; }
  public Integer getMinPlayers() { return minPlayers; }
  public Integer getMaxPlayers() { return maxPlayers; }
  public BigDecimal getPrice() { return price; }
  public String getCurrency() { return currency; }
  public String getImageUrl() { return imageUrl; }
  public String getContactUrl() { return contactUrl; }
  public String getStatus() { return status; }
  public Instant getPublishedAt() { return publishedAt; }

  public void setStatus(String status) {
    this.status = status;
    if ("published".equals(status) && publishedAt == null) publishedAt = Instant.now();
    if ("completed".equals(status) || "archived".equals(status)) completedAt = Instant.now();
    if ("cancelled".equals(status)) cancelledAt = Instant.now();
  }

  public void updateTitle(String value) { title = value; }
  public void updateDescription(String value) { description = value; }
  public void updateGameSystem(String value) { gameSystem = value; }
  public void updateExperienceLevel(String value) { experienceLevel = value; }
  public void updateAgeRating(String value) { ageRating = value; }
  public void updateDateTimeStart(Instant value) {
    dateTimeStart = value;
    refreshDateTimeEnd();
  }
  public void updateDurationMinutes(Integer value) {
    durationMinutes = value;
    refreshDateTimeEnd();
  }
  public void updatePlayers(Integer minPlayers, Integer maxPlayers) {
    this.minPlayers = minPlayers;
    this.maxPlayers = maxPlayers;
  }
  public void updatePrice(BigDecimal price, String currency) {
    this.price = price;
    if (currency != null && !currency.isBlank()) this.currency = currency;
  }
  public void updateContactUrl(String value) { contactUrl = value; }

  private void refreshDateTimeEnd() {
    dateTimeEnd = durationMinutes == null ? null : dateTimeStart.plusSeconds(durationMinutes.longValue() * 60);
  }
}
