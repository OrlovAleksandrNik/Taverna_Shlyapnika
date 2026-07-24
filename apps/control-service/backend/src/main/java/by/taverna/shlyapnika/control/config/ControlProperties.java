package by.taverna.shlyapnika.control.config;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "control")
public record ControlProperties(
    @NotBlank String publicUrl,
    @NotBlank String frontendOrigin,
    @NotBlank String sessionSecret,
    @NotBlank String encryptionKey,
    boolean publicRegistrationEnabled,
    boolean mainSiteIntegrationEnabled,
    boolean telegramIntegrationEnabled,
    boolean desktopAgentEnabled,
    @NotBlank String mediaStorageRoot,
    @NotBlank String backupStorageRoot,
    String bootstrapOwnerEmail,
    String bootstrapToken,
    Mail mail,
    DesktopAgent desktopAgent
) {
  public record Mail(String provider, String host, int port, String username, String password, String from, boolean startTls) {
  }

  public record DesktopAgent(Map<String, String> launchPaths) {
  }
}
