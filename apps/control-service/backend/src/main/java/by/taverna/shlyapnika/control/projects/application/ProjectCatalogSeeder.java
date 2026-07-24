package by.taverna.shlyapnika.control.projects.application;

import by.taverna.shlyapnika.control.projects.domain.ManagedProject;
import by.taverna.shlyapnika.control.projects.infrastructure.ManagedProjectRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProjectCatalogSeeder implements ApplicationRunner {
  private final ManagedProjectRepository projects;

  public ProjectCatalogSeeder(ManagedProjectRepository projects) {
    this.projects = projects;
  }

  @Override
  public void run(ApplicationArguments args) {
    seed("voicemod", "VoiceMod Panel", "desktop-helper", "../../voicemod-panel-d-work", "Node.js ESM + static HTML/CSS/JS", "MOCK_READY",
        "Allowlisted project. Current control service never launches arbitrary paths from browser input.");
    seed("screenstage", "ScreenStage", "desktop-helper", "../../ScreenStage-redesign", ".NET 8 WPF + LibVLCSharp", "MOCK_READY",
        "Detected as ScreenStage-redesign. Real launch requires a separately authenticated Desktop Agent.");
  }

  private void seed(String code, String name, String kind, String path, String stack, String status, String notes) {
    if (!projects.existsByCode(code)) {
      projects.save(new ManagedProject(code, name, kind, path, stack, status, notes));
    }
  }
}
