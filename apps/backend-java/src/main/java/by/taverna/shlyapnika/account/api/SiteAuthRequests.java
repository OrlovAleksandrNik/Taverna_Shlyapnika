package by.taverna.shlyapnika.account.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SiteAuthRequests {
  private SiteAuthRequests() {
  }

  public record RegisterRequest(
      @NotBlank @Size(min = 2, max = 80) String displayName,
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(min = 8, max = 160) String password,
      @NotBlank @Size(min = 8, max = 160) String passwordConfirmation,
      @NotBlank @Size(max = 20) String accountType,
      @Size(max = 80) String telegramUsername,
      Boolean consentGiven,
      String consentVersion,
      String privacyPolicyVersion
  ) {
  }

  public record LoginRequest(
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(max = 160) String password
  ) {
  }
}
