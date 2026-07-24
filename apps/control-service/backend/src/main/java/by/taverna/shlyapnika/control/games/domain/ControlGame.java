package by.taverna.shlyapnika.control.games.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_games")
public class ControlGame {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(nullable = false)
  private String title;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;
  @Column(name = "game_system", nullable = false)
  private String gameSystem;
  @Column(name = "experience_level", nullable = false)
  private String experienceLevel;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameStatus status = GameStatus.DRAFT;
  @Column(name = "master_public_id")
  private String masterPublicId;
  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;
  @Column(name = "duration_minutes", nullable = false)
  private int durationMinutes;
  @Column(name = "min_players", nullable = false)
  private int minPlayers;
  @Column(name = "max_players", nullable = false)
  private int maxPlayers;
  @Column(nullable = false)
  private BigDecimal price;
  @Column(name = "image_url")
  private String imageUrl;
  @Column(name = "staff_notes", columnDefinition = "TEXT")
  private String staffNotes;
  @Column(name = "deleted_at")
  private Instant deletedAt;
  @Column(name = "published_at")
  private Instant publishedAt;
  @Version
  private Long version;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ControlGame() {
  }

  public ControlGame(String title, String description, String gameSystem, String experienceLevel, Instant startsAt,
      int durationMinutes, int minPlayers, int maxPlayers, BigDecimal price) {
    this.title = title;
    this.description = description;
    this.gameSystem = gameSystem;
    this.experienceLevel = experienceLevel;
    this.startsAt = startsAt;
    this.durationMinutes = durationMinutes;
    this.minPlayers = minPlayers;
    this.maxPlayers = maxPlayers;
    this.price = price;
  }

  public UUID getId() { return id; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public String getGameSystem() { return gameSystem; }
  public String getExperienceLevel() { return experienceLevel; }
  public GameStatus getStatus() { return status; }
  public String getMasterPublicId() { return masterPublicId; }
  public Instant getStartsAt() { return startsAt; }
  public int getDurationMinutes() { return durationMinutes; }
  public int getMinPlayers() { return minPlayers; }
  public int getMaxPlayers() { return maxPlayers; }
  public BigDecimal getPrice() { return price; }
  public String getImageUrl() { return imageUrl; }
  public String getStaffNotes() { return staffNotes; }
  public Long getVersion() { return version; }

  public void update(String title, String description, String gameSystem, String experienceLevel, Instant startsAt,
      int durationMinutes, int minPlayers, int maxPlayers, BigDecimal price, String masterPublicId, String staffNotes) {
    this.title = title;
    this.description = description;
    this.gameSystem = gameSystem;
    this.experienceLevel = experienceLevel;
    this.startsAt = startsAt;
    this.durationMinutes = durationMinutes;
    this.minPlayers = minPlayers;
    this.maxPlayers = maxPlayers;
    this.price = price;
    this.masterPublicId = masterPublicId;
    this.staffNotes = staffNotes;
    this.updatedAt = Instant.now();
  }

  public void publish() {
    status = GameStatus.PUBLISHED;
    publishedAt = Instant.now();
    updatedAt = Instant.now();
  }

  public void cancel() {
    status = GameStatus.CANCELLED;
    updatedAt = Instant.now();
  }

  public void softDelete() {
    deletedAt = Instant.now();
    status = GameStatus.ARCHIVED;
    updatedAt = Instant.now();
  }
}
