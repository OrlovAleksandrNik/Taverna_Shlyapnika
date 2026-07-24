package by.taverna.shlyapnika.control.auth.api;

import by.taverna.shlyapnika.control.auth.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public final class AuthDtos {
  private AuthDtos() {
  }

  public record LoginRequest(@Email String email, @NotBlank String password, String twoFactorCode) {
  }

  public record InviteRequest(@Email String email, @NotBlank String displayName, @NotNull UserRole role) {
  }

  public record InvitationResponse(String id, String oneTimeToken, Instant expiresAt) {
  }

  public record AcceptInvitationRequest(@NotBlank String token, @Size(min = 12) String password) {
  }

  public record AccountResponse(
      String publicId,
      String displayName,
      String email,
      Set<UserRole> roles,
      Set<String> permissions,
      boolean twoFactorEnabled,
      Instant lastLoginAt
  ) {
  }
}
