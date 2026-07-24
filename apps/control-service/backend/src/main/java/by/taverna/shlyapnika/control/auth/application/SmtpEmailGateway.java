package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "control.mail", name = "provider", havingValue = "smtp")
public class SmtpEmailGateway implements EmailGateway {
  private final JavaMailSender mailSender;
  private final ControlProperties properties;

  public SmtpEmailGateway(JavaMailSender mailSender, ControlProperties properties) {
    this.mailSender = mailSender;
    this.properties = properties;
  }

  @Override
  public void sendInvitation(String email, String displayName, String token, Instant expiresAt) {
    send(email, "Taverna Control invitation", """
        Hello %s,

        You were invited to Taverna Control.
        One-time token: %s
        Expires at: %s
        """.formatted(displayName, token, expiresAt));
  }

  @Override
  public void sendEmailVerification(String email, String token, Instant expiresAt) {
    send(email, "Verify your Taverna Control email", """
        Confirm your email in Taverna Control.
        Verification token: %s
        Expires at: %s
        """.formatted(token, expiresAt));
  }

  @Override
  public void sendPasswordReset(String email, String token, Instant expiresAt) {
    send(email, "Reset your Taverna Control password", """
        Password reset token: %s
        Expires at: %s
        """.formatted(token, expiresAt));
  }

  @Override
  public void sendSecurityAlert(String email, String subject, String message) {
    send(email, subject, message);
  }

  private void send(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(properties.mail().from());
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }
}
