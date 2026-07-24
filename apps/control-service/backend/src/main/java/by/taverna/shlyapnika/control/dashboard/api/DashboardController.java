package by.taverna.shlyapnika.control.dashboard.api;

import by.taverna.shlyapnika.control.dashboard.application.DashboardService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {
  private final DashboardService dashboard;

  public DashboardController(DashboardService dashboard) {
    this.dashboard = dashboard;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('games.read')")
  Map<String, Object> snapshot() {
    return dashboard.snapshot();
  }
}
