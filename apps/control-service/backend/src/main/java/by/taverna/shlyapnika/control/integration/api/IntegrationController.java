package by.taverna.shlyapnika.control.integration.api;

import by.taverna.shlyapnika.control.config.ControlProperties;
import by.taverna.shlyapnika.control.projects.application.DesktopAgentLauncher;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/integration")
public class IntegrationController {
  private final ControlProperties properties;
  private final DesktopAgentLauncher desktopAgentLauncher;

  public IntegrationController(ControlProperties properties, DesktopAgentLauncher desktopAgentLauncher) {
    this.properties = properties;
    this.desktopAgentLauncher = desktopAgentLauncher;
  }

  @GetMapping("/status")
  @PreAuthorize("hasAuthority('integrations.read')")
  Map<String, Object> status() {
    return Map.of(
        "mode", "isolated",
        "mainSiteIntegrationEnabled", properties.mainSiteIntegrationEnabled(),
        "telegramIntegrationEnabled", properties.telegramIntegrationEnabled(),
        "desktopAgentEnabled", properties.desktopAgentEnabled(),
        "desktopAgent", desktopAgentLauncher.diagnostics(),
        "contractsPrepared", true,
        "productionDataUsed", false);
  }
}
