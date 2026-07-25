package by.taverna.shlyapnika.characters.character;

import by.taverna.shlyapnika.characters.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_sheets")
public class CharacterSheet {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "public_id", nullable = false, unique = true)
  private String publicId = Ids.newId("chr");
  @Column(name = "owner_account_id", nullable = false)
  private String ownerAccountId;
  @Column(name = "player_public_id")
  private String playerPublicId;
  @Column(name = "game_system", nullable = false)
  private String gameSystem = "dnd5e";
  @Column(name = "sheet_type", nullable = false)
  private String sheetType = "dnd5e";
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CharacterVisibility visibility = CharacterVisibility.PRIVATE;
  @Column(nullable = false)
  private String name;
  private String ancestry;
  @Column(name = "class_name")
  private String className;
  @Column(nullable = false)
  private int level = 1;
  @Column(nullable = false)
  private int experience = 0;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String payload = "{}";
  @Column(name = "deleted_at")
  private Instant deletedAt;
  @Version
  private Long version;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected CharacterSheet() {
  }

  public CharacterSheet(String ownerAccountId, String name) {
    this.ownerAccountId = ownerAccountId;
    this.name = name;
  }

  public UUID getId() { return id; }
  public String getPublicId() { return publicId; }
  public String getOwnerAccountId() { return ownerAccountId; }
  public String getPlayerPublicId() { return playerPublicId; }
  public String getGameSystem() { return gameSystem; }
  public String getSheetType() { return sheetType; }
  public CharacterVisibility getVisibility() { return visibility; }
  public String getName() { return name; }
  public String getAncestry() { return ancestry; }
  public String getClassName() { return className; }
  public int getLevel() { return level; }
  public int getExperience() { return experience; }
  public String getPayload() { return payload; }
  public Instant getDeletedAt() { return deletedAt; }
  public Long getVersion() { return version; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void update(String ownerAccountId, String playerPublicId, String gameSystem, String sheetType,
      CharacterVisibility visibility, String name, String ancestry, String className, int level, int experience,
      String payload) {
    this.ownerAccountId = ownerAccountId;
    this.playerPublicId = playerPublicId;
    this.gameSystem = gameSystem;
    this.sheetType = sheetType;
    this.visibility = visibility;
    this.name = name;
    this.ancestry = ancestry;
    this.className = className;
    this.level = level;
    this.experience = experience;
    this.payload = payload;
    this.updatedAt = Instant.now();
  }

  public void softDelete() {
    deletedAt = Instant.now();
    updatedAt = Instant.now();
  }
}
