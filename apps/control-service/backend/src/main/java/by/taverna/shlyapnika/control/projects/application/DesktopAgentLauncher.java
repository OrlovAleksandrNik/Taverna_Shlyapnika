package by.taverna.shlyapnika.control.projects.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import by.taverna.shlyapnika.control.projects.domain.ManagedProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DesktopAgentLauncher {
  private final ControlProperties properties;

  public DesktopAgentLauncher(ControlProperties properties) {
    this.properties = properties;
  }

  public LaunchResult launch(ManagedProject project) {
    if (!properties.desktopAgentEnabled()) {
      return LaunchResult.mock(project.getCode(), "Desktop Agent is disabled.");
    }
    String configuredPath = configuredPath(project.getCode());
    if (configuredPath == null || configuredPath.isBlank()) {
      return LaunchResult.blocked(project.getCode(), "No allowlisted launch path is configured for this project.");
    }
    Path executable = Path.of(configuredPath).toAbsolutePath().normalize();
    if (!Files.isRegularFile(executable)) {
      return LaunchResult.blocked(project.getCode(), "Allowlisted launch path does not exist: " + executable);
    }
    String fileName = executable.getFileName().toString().toLowerCase(Locale.ROOT);
    if (!(fileName.endsWith(".exe") || fileName.endsWith(".cmd") || fileName.endsWith(".bat"))) {
      return LaunchResult.blocked(project.getCode(), "Allowlisted launch path must point to .exe, .cmd or .bat.");
    }
    try {
      Process process = new ProcessBuilder(executable.toString())
          .directory(executable.getParent().toFile())
          .start();
      return LaunchResult.started(project.getCode(), process.pid(), executable.toString());
    } catch (Exception e) {
      return LaunchResult.blocked(project.getCode(), "Launch failed: " + e.getMessage());
    }
  }

  public Map<String, Object> diagnostics() {
    Map<String, String> paths = properties.desktopAgent() == null ? Map.of() : properties.desktopAgent().launchPaths();
    return Map.of(
        "enabled", properties.desktopAgentEnabled(),
        "allowlistedProjects", paths == null ? Map.of() : paths,
        "browserPathInputAccepted", false,
        "allowedExtensions", ".exe, .cmd, .bat");
  }

  private String configuredPath(String projectCode) {
    if (properties.desktopAgent() == null || properties.desktopAgent().launchPaths() == null) return null;
    return properties.desktopAgent().launchPaths().get(projectCode);
  }

  public record LaunchResult(String code, String status, boolean desktopAgentRequired, Instant startedAt, Long pid, String path, String message) {
    static LaunchResult mock(String code, String message) {
      return new LaunchResult(code, "mock-launch-recorded", true, Instant.now(), null, null, message);
    }

    static LaunchResult blocked(String code, String message) {
      return new LaunchResult(code, "launch-blocked", true, Instant.now(), null, null, message);
    }

    static LaunchResult started(String code, long pid, String path) {
      return new LaunchResult(code, "started", false, Instant.now(), pid, path, "Allowlisted desktop project started.");
    }
  }
}
