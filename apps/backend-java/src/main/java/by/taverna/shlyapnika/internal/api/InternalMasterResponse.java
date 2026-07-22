package by.taverna.shlyapnika.internal.api;

public record InternalMasterResponse(
    String id,
    Long telegramUserId,
    String telegramUsername,
    String displayName,
    String contactUrl,
    String role,
    String status
) {
}
