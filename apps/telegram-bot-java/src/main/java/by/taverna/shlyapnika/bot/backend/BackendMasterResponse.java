package by.taverna.shlyapnika.bot.backend;

public record BackendMasterResponse(
    String id,
    long telegramUserId,
    String telegramUsername,
    String displayName,
    String contactUrl,
    String role,
    String status
) {
  public boolean active() {
    return "active".equals(status);
  }
}
