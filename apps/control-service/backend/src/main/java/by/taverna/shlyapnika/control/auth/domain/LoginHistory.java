package by.taverna.shlyapnika.control.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_login_history")
public class LoginHistory {
  @Id
  private UUID id = UUID.randomUUID();
  @Column(name = "user_id")
  private UUID userId;
  @Column(nullable = false)
  private String email;
  @Column(nullable = false)
  private boolean success;
  private String reason;
  @Column(name = "ip_address")
  private String ipAddress;
  @Column(name = "user_agent")
  private String userAgent;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected LoginHistory() {
  }

  public LoginHistory(UUID userId, String email, boolean success, String reason, String ipAddress, String userAgent) {
    this.userId = userId;
    this.email = email.toLowerCase();
    this.success = success;
    this.reason = reason;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
  }
}
