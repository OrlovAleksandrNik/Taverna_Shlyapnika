package by.taverna.shlyapnika.account.api;

import by.taverna.shlyapnika.account.SiteAccountService;
import by.taverna.shlyapnika.account.SiteAccountService.AuthenticatedAccount;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.LoginRequest;
import by.taverna.shlyapnika.account.api.SiteAuthRequests.RegisterRequest;
import by.taverna.shlyapnika.access.api.MasterSessionResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteAuthController {
  private final SiteAccountService service;

  public SiteAuthController(SiteAccountService service) {
    this.service = service;
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

  private static void writeSession(HttpSession session, AuthenticatedAccount account) {
    var systemRole = systemRole(account.role());
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", account.displayName());
    session.setAttribute("taverna.master.role", legacyRole(account.role()));
    session.setAttribute("taverna.master.baseRole", legacyRole(account.role()));
    session.setAttribute("taverna.master.profileMode", account.role());
    session.setAttribute("taverna.master.telegramUsername", account.telegramUsername());
    session.setAttribute("taverna.master.email", account.email());
    session.setAttribute("taverna.auth.accountId", account.id());
    session.setAttribute("taverna.auth.accountType", account.role());
    session.setAttribute("taverna.auth.status", account.status());
    session.setAttribute("taverna.auth.systemRole", systemRole);
    session.setAttribute("taverna.auth.activeProfile", account.role());
  }

  private static MasterSessionResponse response(AuthenticatedAccount account, boolean accessGranted) {
    return new MasterSessionResponse(
        accessGranted,
        account.displayName(),
        legacyRole(account.role()),
        legacyRole(account.role()),
        account.role(),
        false,
        account.telegramUsername(),
        account.email(),
        account.id(),
        account.role(),
        account.status(),
        systemRole(account.role()),
        account.role()
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
