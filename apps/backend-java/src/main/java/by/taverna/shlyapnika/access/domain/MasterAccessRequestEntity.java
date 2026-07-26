package by.taverna.shlyapnika.access.domain;

import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.schedule.domain.ConsentSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "\"MasterAccessRequest\"")
public class MasterAccessRequestEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @Column(name = "\"displayName\"", nullable = false)
  private String displayName;

  @Column(name = "\"email\"", nullable = false)
  private String email;

  @Column(name = "\"telegramUsername\"", nullable = false)
  private String telegramUsername;

  @Column(name = "\"normalizedTelegramUsername\"", nullable = false)
  private String normalizedTelegramUsername;

  @Column(name = "\"requestedRole\"", nullable = false)
  private String requestedRole = "master";

  @Column(name = "\"status\"", nullable = false)
  @ColumnTransformer(write = "?::\"MasterAccessRequestStatus\"")
  private String status = "pending";

  @Column(name = "\"consentGiven\"", nullable = false)
  private Boolean consentGiven = false;

  @Column(name = "\"consentVersion\"", nullable = false)
  private String consentVersion = "legacy";

  @Column(name = "\"privacyPolicyVersion\"", nullable = false)
  private String privacyPolicyVersion = "legacy";

  @Column(name = "\"consentedAt\"")
  private Instant consentedAt;

  @Column(name = "\"formType\"", nullable = false)
  private String formType = "master-registration";

  @Column(name = "\"decidedAt\"")
  private Instant decidedAt;

  @Column(name = "\"decidedByTelegramId\"")
  private Long decidedByTelegramId;

  @Column(name = "\"decisionComment\"")
  private String decisionComment;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public static MasterAccessRequestEntity create(
      String displayName,
      String email,
      String telegramUsername,
      String normalizedTelegramUsername,
      ConsentSnapshot consent
  ) {
    var request = new MasterAccessRequestEntity();
    request.displayName = displayName.trim();
    request.email = email.trim();
    request.telegramUsername = telegramUsername.trim();
    request.normalizedTelegramUsername = normalizedTelegramUsername;
    request.consentGiven = true;
    request.consentVersion = consent.consentVersion();
    request.privacyPolicyVersion = consent.privacyPolicyVersion();
    request.consentedAt = consent.consentedAt();
    request.formType = consent.formType();
    return request;
  }

  public void approve(Long adminTelegramId, String comment) {
    status = "approved";
    decidedAt = Instant.now();
    decidedByTelegramId = adminTelegramId;
    decisionComment = blankToNull(comment);
  }

  public void reject(Long adminTelegramId, String comment) {
    status = "rejected";
    decidedAt = Instant.now();
    decidedByTelegramId = adminTelegramId;
    decisionComment = blankToNull(comment);
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("mac");
    var now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }

  public String getTelegramUsername() {
    return telegramUsername;
  }

  public String getNormalizedTelegramUsername() {
    return normalizedTelegramUsername;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
