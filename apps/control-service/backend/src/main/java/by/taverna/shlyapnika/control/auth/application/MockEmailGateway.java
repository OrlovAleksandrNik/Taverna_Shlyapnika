package by.taverna.shlyapnika.control.auth.application;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "control.mail", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailGateway implements EmailGateway {
  private static final Logger log = LoggerFactory.getLogger(MockEmailGateway.class);

  @Override
  public void sendInvitation(String email, String displayName, String token, Instant expiresAt) {
    log.info("Mock invitation email queued for {} until {} with token {}", email, expiresAt, token);
  }

  @Override
  public void sendEmailVerification(String email, String token, Instant expiresAt) {
    log.info("Mock verification email queued for {} until {} with token {}", email, expiresAt, token);
  }

  @Override
  public void sendPasswordReset(String email, String token, Instant expiresAt) {
    log.info("Mock password reset email queued for {} until {} with token {}", email, expiresAt, token);
  }

  @Override
  public void sendSecurityAlert(String email, String subject, String message) {
    log.info("Mock security alert queued for {}: {} - {}", email, subject, message);
  }
}
