package by.taverna.shlyapnika.control.auth.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapOwnerRunner implements ApplicationRunner {
  private final AuthService authService;

  public BootstrapOwnerRunner(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public void run(ApplicationArguments args) {
    authService.bootstrapOwnerIfConfigured();
  }
}
