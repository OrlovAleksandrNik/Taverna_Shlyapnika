package by.taverna.shlyapnika.access.api;

public record MasterSessionResponse(
    boolean accessGranted,
    String displayName,
    String role,
    String telegramUsername,
    String email
) {
  public static MasterSessionResponse anonymous() {
    return new MasterSessionResponse(false, null, "master", null, null);
  }
}
