package by.taverna.shlyapnika.application.domain;

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
@Table(name = "\"ServiceRequest\"")
public class ServiceRequestEntity {
  @Id
  @Column(name = "\"id\"", nullable = false)
  private String id;

  @Column(name = "\"name\"", nullable = false)
  private String name;

  @Column(name = "\"contact\"", nullable = false)
  private String contact;

  @Column(name = "\"service\"", nullable = false)
  private String service;

  @Column(name = "\"desiredDate\"")
  private String desiredDate;

  @Column(name = "\"participants\"")
  private Integer participants;

  @Column(name = "\"city\"")
  private String city;

  @Column(name = "\"comment\"")
  private String comment;

  @Column(name = "\"status\"", nullable = false)
  @ColumnTransformer(write = "?::\"ServiceRequestStatus\"")
  private String status = "new";

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

  public static ServiceRequestEntity create(
      String name,
      String contact,
      String service,
      String desiredDate,
      Integer participants,
      String city,
      String comment,
      ConsentSnapshot consent
  ) {
    var entity = new ServiceRequestEntity();
    entity.name = name;
    entity.contact = contact;
    entity.service = service;
    entity.desiredDate = blankToNull(desiredDate);
    entity.participants = participants;
    entity.city = blankToNull(city);
    entity.comment = blankToNull(comment);
    entity.consentGiven = true;
    entity.consentVersion = consent.consentVersion();
    entity.privacyPolicyVersion = consent.privacyPolicyVersion();
    entity.consentedAt = consent.consentedAt();
    entity.formType = consent.formType();
    return entity;
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = Ids.newId("srv");
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

  public String getService() {
    return service;
  }
}
