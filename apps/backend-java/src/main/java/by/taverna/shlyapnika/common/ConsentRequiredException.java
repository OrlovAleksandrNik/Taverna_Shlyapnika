package by.taverna.shlyapnika.common;

public class ConsentRequiredException extends RuntimeException {
  public static final String CODE = "CONSENT_REQUIRED";

  public ConsentRequiredException() {
    super("Заявка не отправлена: необходимо согласие на обработку персональных данных.");
  }
}
