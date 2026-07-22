package by.taverna.shlyapnika.bot.backend;

public record BackendMasterRequest(
    long telegramUserId,
    String telegramUsername,
    String displayName,
    String contactUrl
) {
}
