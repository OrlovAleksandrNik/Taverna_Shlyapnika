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
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "\"GameSignup\"")
public class GameSignupEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"gameId\"", nullable = false)
  private GameEntity game;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"masterId\"", nullable = false)
  private MasterEntity master;

  @Column(name = "\"playerName\"", nullable = false)
  private String playerName;

  @Column(name = "\"contact\"", nullable = false)
  private String contact;

  @Column(name = "\"seats\"", nullable = false)
  private Integer seats = 1;

  @Column(name = "\"comment\"")
  private String comment;

  @Column(name = "\"status\"", nullable = false)
  @ColumnTransformer(write = "?::\"SignupStatus\"")
  private String status = "confirmed";

  @Column(name = "\"consentGiven\"", nullable = false)
  private Boolean consentGiven = false;

  @Column(name = "\"consentVersion\"", nullable = false)
  private String consentVersion = "legacy";

  @Column(name = "\"privacyPolicyVersion\"", nullable = false)
  private String privacyPolicyVersion = "legacy";

  @Column(name = "\"consentedAt\"")
  private Instant consentedAt;

  @Column(name = "\"formType\"", nullable = false)
  private String formType = "legacy";

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public static GameSignupEntity confirmed(GameEntity game, String playerName, String contact, int seats, String comment, ConsentSnapshot consent) {
    var signup = new GameSignupEntity();
    signup.game = game;
    signup.master = game.getMaster();
    signup.playerName = playerName;
    signup.contact = contact;
    signup.seats = seats;
    signup.comment = comment;
    signup.status = "confirmed";
    signup.consentGiven = true;
    signup.consentVersion = consent.consentVersion();
    signup.privacyPolicyVersion = consent.privacyPolicyVersion();
    signup.consentedAt = consent.consentedAt();
    signup.formType = consent.formType();
    return signup;
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("sgn");
    var now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public String getId() { return id; }
  public Integer getSeats() { return seats; }
  public void updateConfirmed(String playerName, int seats, String comment, ConsentSnapshot consent) {
    this.playerName = playerName;
    this.seats = seats;
    this.comment = comment;
    this.status = "confirmed";
    this.consentGiven = true;
    this.consentVersion = consent.consentVersion();
    this.privacyPolicyVersion = consent.privacyPolicyVersion();
    this.consentedAt = consent.consentedAt();
    this.formType = consent.formType();
  }
}
