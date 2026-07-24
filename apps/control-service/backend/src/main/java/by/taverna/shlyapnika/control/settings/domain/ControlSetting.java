package by.taverna.shlyapnika.control.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "control_settings")
public class ControlSetting {
  @Id
  private String key;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String value;
  @Column(nullable = false)
  private boolean sensitive;
  @Column(nullable = false)
  private boolean encrypted;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ControlSetting() {
  }

  public ControlSetting(String key, String value, boolean sensitive, boolean encrypted) {
    this.key = key;
    this.value = value;
    this.sensitive = sensitive;
    this.encrypted = encrypted;
  }

  public String getKey() { return key; }
  public String getValue() { return sensitive ? "********" : value; }
  public boolean isSensitive() { return sensitive; }
  public boolean isEncrypted() { return encrypted; }
  public Instant getUpdatedAt() { return updatedAt; }
}
