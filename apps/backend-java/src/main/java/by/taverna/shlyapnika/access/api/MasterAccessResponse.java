package by.taverna.shlyapnika.access.api;

public record MasterAccessResponse(
    boolean ok,
    String message,
    String requestId,
    String status,
    boolean accessGranted,
    String displayName,
    String role
) {
  public static MasterAccessResponse requested(String requestId, String status, String message) {
    return new MasterAccessResponse(true, message, requestId, status, false, null, "master");
  }

  public static MasterAccessResponse approved(String requestId, String message, String displayName, String role) {
    return new MasterAccessResponse(true, message, requestId, "approved", true, displayName, role);
  }

  public static MasterAccessResponse login(boolean accessGranted, String message, String displayName, String role) {
    return new MasterAccessResponse(accessGranted, message, null, null, accessGranted, displayName, role);
  }
}
