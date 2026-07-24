package by.taverna.shlyapnika.control.auth.application;

import java.time.Instant;

public interface EmailGateway {
  void sendInvitation(String email, String displayName, String token, Instant expiresAt);
  void sendEmailVerification(String email, String token, Instant expiresAt);
  void sendPasswordReset(String email, String token, Instant expiresAt);
  void sendSecurityAlert(String email, String subject, String message);
}
