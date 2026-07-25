package by.taverna.shlyapnika.characters.rolls;

import by.taverna.shlyapnika.characters.character.CharacterSheet;
import by.taverna.shlyapnika.characters.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_roll_events")
public class RollEvent {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "public_id", nullable = false, unique = true)
  private String publicId = Ids.newId("roll");
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "character_id", nullable = false)
  private CharacterSheet character;
  @Column(name = "actor_account_id")
  private String actorAccountId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RollKind kind;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RollMode mode;
  @Column(nullable = false)
  private String label;
  @Column(nullable = false)
  private String formula;
  @Column(name = "resolved_formula", nullable = false)
  private String resolvedFormula;
  @Column(nullable = false)
  private int total;
  @Column(name = "detail_json", nullable = false, columnDefinition = "TEXT")
  private String detailJson;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected RollEvent() {
  }

  public RollEvent(CharacterSheet character, String actorAccountId, RollKind kind, RollMode mode, String label,
      String formula, String resolvedFormula, int total, String detailJson) {
    this.character = character;
    this.actorAccountId = actorAccountId;
    this.kind = kind;
    this.mode = mode;
    this.label = label;
    this.formula = formula;
    this.resolvedFormula = resolvedFormula;
    this.total = total;
    this.detailJson = detailJson;
  }

  public UUID getId() { return id; }
  public String getPublicId() { return publicId; }
  public CharacterSheet getCharacter() { return character; }
  public String getActorAccountId() { return actorAccountId; }
  public RollKind getKind() { return kind; }
  public RollMode getMode() { return mode; }
  public String getLabel() { return label; }
  public String getFormula() { return formula; }
  public String getResolvedFormula() { return resolvedFormula; }
  public int getTotal() { return total; }
  public String getDetailJson() { return detailJson; }
  public Instant getCreatedAt() { return createdAt; }
}
