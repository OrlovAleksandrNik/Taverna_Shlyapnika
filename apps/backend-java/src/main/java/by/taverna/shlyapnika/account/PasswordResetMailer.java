package by.taverna.shlyapnika.account;

import by.taverna.shlyapnika.config.TavernaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailer {
  private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

  private final JavaMailSender mailSender;
  private final Environment environment;
  private final TavernaProperties properties;

  public PasswordResetMailer(JavaMailSender mailSender, Environment environment, TavernaProperties properties) {
    this.mailSender = mailSender;
    this.environment = environment;
    this.properties = properties;
  }

  public void sendResetLink(String email, String token) {
    var smtpHost = trimToNull(environment.getProperty("SMTP_HOST"));
    if (smtpHost == null) {
      log.warn("Password reset email was created but SMTP_HOST is not configured");
      return;
    }
    var baseUrl = trimToNull(environment.getProperty("APP_BASE_URL"));
    if (baseUrl == null) baseUrl = properties.siteBaseUrl();
    var from = trimToNull(environment.getProperty("MAIL_FROM"));
    if (from == null) from = "no-reply@taverna.local";
    var resetUrl = baseUrl.replaceAll("/+$", "") + "/reset-password.html?token=" + token;

    var message = new SimpleMailMessage();
    message.setTo(email);
    message.setFrom(from);
    message.setSubject("Восстановление пароля Таверны Шляпника");
    message.setText(String.join("\n",
        "Вы запросили восстановление пароля для аккаунта Таверны Шляпника.",
        "",
        "Сменить пароль можно по ссылке:",
        resetUrl,
        "",
        "Ссылка действует 45 минут и может быть использована только один раз.",
        "Если вы ничего не запрашивали, просто проигнорируйте это письмо."
    ));
    mailSender.send(message);
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
