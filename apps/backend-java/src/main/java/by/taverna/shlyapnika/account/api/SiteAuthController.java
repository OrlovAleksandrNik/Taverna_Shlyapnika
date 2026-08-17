package by.taverna.shlyapnika.account.api;

import by.taverna.shlyapnika.account.SiteAccountService;
import by.taverna.shlyapnika.account.SiteAccountService.AuthenticatedAccount;
import by.taverna.shlyapnika.account.PasswordResetService;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.ForgotPasswordRequest;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.LoginRequest;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.RegisterRequest;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.ResetPasswordRequest;
import by.taverna.shlyapnika.access.api.MasterSessionResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteAuthController {
  private final SiteAccountService service;
  private final PasswordResetService passwordResetService;

  public SiteAuthController(SiteAccountService service, PasswordResetService passwordResetService) {
    this.service = service;
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/api/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  public MasterSessionResponse register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
    var account = service.register(request);
    if ("active".equals(account.status())) {
      writeSession(session, account);
      return response(account, true);
    }
    return response(account, false);
  }

  @PostMapping("/api/auth/login")
  public MasterSessionResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
    var account = service.login(request);
    if (!"active".equals(account.status())) {
      return response(account, false);
    }
    writeSession(session, account);
    return response(account, true);
  }

  @PostMapping("/api/auth/forgot-password")
  public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    return Map.of("message", passwordResetService.requestReset(request.email()));
  }

  @PostMapping("/api/auth/reset-password")
  public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.password(), request.passwordConfirmation());
    return Map.of("message", "Пароль успешно изменён.");
  }

  private static void writeSession(HttpSession session, AuthenticatedAccount account) {
    var systemRole = systemRole(account.role());
    var hatter = "HATTER".equals(systemRole);
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", hatter ? "Шляпник" : account.displayName());
    session.setAttribute("taverna.master.role", hatter ? "admin" : legacyRole(account.role()));
    session.setAttribute("taverna.master.baseRole", legacyRole(account.role()));
    session.setAttribute("taverna.master.profileMode", hatter ? "hatter" : account.role());
    session.setAttribute("taverna.master.telegramUsername", account.telegramUsername());
    session.setAttribute("taverna.master.email", account.email());
    session.setAttribute("taverna.auth.accountId", account.id());
    session.setAttribute("taverna.auth.accountType", account.role());
    session.setAttribute("taverna.auth.status", account.status());
    session.setAttribute("taverna.auth.systemRole", systemRole);
    session.setAttribute("taverna.auth.activeProfile", hatter ? "hatter" : account.role());
    session.setAttribute("taverna.auth.sessionVersion", account.sessionVersion());
  }

  private static MasterSessionResponse response(AuthenticatedAccount account, boolean accessGranted) {
    var systemRole = systemRole(account.role());
    var hatter = "HATTER".equals(systemRole);
    return new MasterSessionResponse(
        accessGranted,
        hatter ? "Шляпник" : account.displayName(),
        hatter ? "admin" : legacyRole(account.role()),
        legacyRole(account.role()),
        hatter ? "hatter" : account.role(),
        hatter,
        account.telegramUsername(),
        account.email(),
        account.id(),
        account.role(),
        account.status(),
        systemRole,
        hatter ? "hatter" : account.role()
    );
  }

  private static String legacyRole(String role) {
    return "hatter".equals(role) ? "admin" : role;
  }

  private static String systemRole(String role) {
    return switch (String.valueOf(role)) {
      case "hatter" -> "HATTER";
      case "master" -> "MASTER";
      case "player" -> "PLAYER";
      default -> "GUEST";
    };
  }
}
