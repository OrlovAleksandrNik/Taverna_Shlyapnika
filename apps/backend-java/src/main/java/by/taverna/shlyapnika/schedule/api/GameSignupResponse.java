package by.taverna.shlyapnika.schedule.api;

public record GameSignupResponse(boolean ok, String message, String signupId, GameResponses.PublicGameDto game) {
}
