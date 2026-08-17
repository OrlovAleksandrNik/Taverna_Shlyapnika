package by.taverna.shlyapnika.access.api;

public record MasterSessionResponse(
    boolean accessGranted,
    String displayName,
    String role,
    String baseRole,
    String profileMode,
    boolean canSwitchProfile,
    String telegramUsername,
    String email,
    String accountId,
    String accountType,
    String status,
    String systemRole,
    String activeProfile
) {
  public static MasterSessionResponse anonymous() {
    return new MasterSessionResponse(false, null, "guest", "guest", "guest", false, null, null, null, "guest", "anonymous", "GUEST", "guest");
  }
}
