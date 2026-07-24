package by.taverna.shlyapnika.control.settings.api;

import by.taverna.shlyapnika.control.config.ControlProperties;
import by.taverna.shlyapnika.control.settings.domain.ControlSetting;
import by.taverna.shlyapnika.control.settings.infrastructure.ControlSettingRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class SettingsController {
  private final ControlProperties properties;
  private final ControlSettingRepository settings;

  public SettingsController(ControlProperties properties, ControlSettingRepository settings) {
    this.properties = properties;
    this.settings = settings;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('settings.manage')")
  Map<String, Object> settings() {
    return Map.of(
        "featureFlags", Map.of(
            "mainSiteIntegration", properties.mainSiteIntegrationEnabled(),
            "telegramIntegration", properties.telegramIntegrationEnabled(),
            "desktopAgent", properties.desktopAgentEnabled(),
            "publicRegistration", properties.publicRegistrationEnabled()),
        "storedSettings", settings.findAll());
  }
}
