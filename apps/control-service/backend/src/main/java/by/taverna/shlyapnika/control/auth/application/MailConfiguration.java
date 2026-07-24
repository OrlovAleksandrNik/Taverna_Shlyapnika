package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import java.util.Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
class MailConfiguration {
  @Bean
  @ConditionalOnProperty(prefix = "control.mail", name = "provider", havingValue = "smtp")
  JavaMailSender controlMailSender(ControlProperties properties) {
    ControlProperties.Mail mail = properties.mail();
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(mail.host());
    sender.setPort(mail.port());
    sender.setUsername(mail.username());
    sender.setPassword(mail.password());
    Properties javaMailProperties = sender.getJavaMailProperties();
    javaMailProperties.put("mail.transport.protocol", "smtp");
    javaMailProperties.put("mail.smtp.auth", String.valueOf(mail.username() != null && !mail.username().isBlank()));
    javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(mail.startTls()));
    return sender;
  }
}
