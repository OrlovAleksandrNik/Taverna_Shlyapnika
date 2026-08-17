package by.taverna.shlyapnika.access.api;

import by.taverna.shlyapnika.access.MasterAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MasterAccessController {
  private final MasterAccessService service;

  public MasterAccessController(MasterAccessService service) {
    this.service = service;
  }

  @PostMapping("/api/auth/master-access-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public MasterAccessResponse requestMasterAccess(@Valid @RequestBody MasterAccessRequest request) {
    return service.requestMasterAccess(request);
  }

  @PostMapping("/api/auth/master-login")
  public MasterAccessResponse login(@Valid @RequestBody MasterLoginRequest request, HttpSession session) {
    var response = service.login(request);
    if (response.accessGranted()) {
      session.setAttribute("taverna.master.accessGranted", true);
      session.setAttribute("taverna.master.displayName", response.displayName());
      session.setAttribute("taverna.master.role", response.role());
      session.setAttribute("taverna.master.baseRole", response.role());
      session.setAttribute("taverna.master.profileMode", isHatter(response.role()) ? "hatter" : "master");
      session.setAttribute("taverna.master.telegramUsername", request.telegramUsername());
      session.setAttribute("taverna.master.email", request.email());
      session.setAttribute("taverna.auth.accountId", response.requestId());
      session.setAttribute("taverna.auth.accountType", isHatter(response.role()) ? "hatter" : "master");
      session.setAttribute("taverna.auth.status", "active");
      session.setAttribute("taverna.auth.systemRole", isHatter(response.role()) ? "HATTER" : "MASTER");
      session.setAttribute("taverna.auth.activeProfile", isHatter(response.role()) ? "hatter" : "master");
    }
    return response;
  }

  @GetMapping("/api/auth/session")
  public MasterSessionResponse session(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session == null) {
      return MasterSessionResponse.anonymous();
    }
    if (!Boolean.TRUE.equals(session.getAttribute("taverna.master.accessGranted"))) {
      return MasterSessionResponse.anonymous();
    }
    var baseRole = stringAttribute(session, "taverna.master.baseRole", stringAttribute(session, "taverna.master.role"));
    if (baseRole != null) {
      session.setAttribute("taverna.master.baseRole", baseRole);
    }
    var profileMode = stringAttribute(session, "taverna.master.profileMode", isHatter(stringAttribute(session, "taverna.master.role")) ? "hatter" : stringAttribute(session, "taverna.auth.accountType", "master"));
    var accountType = stringAttribute(session, "taverna.auth.accountType", isHatter(baseRole) ? "hatter" : profileMode);
    var systemRole = stringAttribute(session, "taverna.auth.systemRole", isHatter(baseRole) ? "HATTER" : systemRole(accountType));
    var activeProfile = stringAttribute(session, "taverna.auth.activeProfile", profileMode);
    return new MasterSessionResponse(
        true,
        stringAttribute(session, "taverna.master.displayName"),
        stringAttribute(session, "taverna.master.role"),
        baseRole,
        profileMode,
        isHatter(baseRole),
        stringAttribute(session, "taverna.master.telegramUsername"),
        stringAttribute(session, "taverna.master.email"),
        stringAttribute(session, "taverna.auth.accountId"),
        accountType,
        stringAttribute(session, "taverna.auth.status", "active"),
        systemRole,
        activeProfile
    );
  }

  @PutMapping("/api/auth/session-mode")
  public ResponseEntity<MasterSessionResponse> switchMode(@Valid @RequestBody MasterSessionModeRequest request, HttpServletRequest servletRequest) {
    var session = servletRequest.getSession(false);
    if (session == null || !Boolean.TRUE.equals(session.getAttribute("taverna.master.accessGranted"))) {
      return ResponseEntity.ok(MasterSessionResponse.anonymous());
    }
    var baseRole = stringAttribute(session, "taverna.master.baseRole", stringAttribute(session, "taverna.master.role"));
    if (!isHatter(baseRole)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    session.setAttribute("taverna.master.baseRole", baseRole);
    if ("hatter".equals(request.mode())) {
      session.setAttribute("taverna.master.role", "admin");
      session.setAttribute("taverna.master.profileMode", "hatter");
      session.setAttribute("taverna.master.displayName", "Шляпник");
      session.setAttribute("taverna.auth.activeProfile", "hatter");
    } else {
      session.setAttribute("taverna.master.role", "master");
      session.setAttribute("taverna.master.profileMode", "master");
      session.setAttribute("taverna.master.displayName", "Мастер Александр");
      session.setAttribute("taverna.auth.activeProfile", "alexander");
    }
    session.setAttribute("taverna.auth.accountType", "hatter");
    session.setAttribute("taverna.auth.status", "active");
    session.setAttribute("taverna.auth.systemRole", "HATTER");
    return ResponseEntity.ok(session(servletRequest));
  }

  @PostMapping("/api/auth/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return ResponseEntity.noContent().build();
  }

  private static String stringAttribute(HttpSession session, String key) {
    var value = session.getAttribute(key);
    return value == null ? null : String.valueOf(value);
  }

  private static String stringAttribute(HttpSession session, String key, String fallback) {
    var value = stringAttribute(session, key);
    return value == null ? fallback : value;
  }

  private static boolean isHatter(String role) {
    return "admin".equalsIgnoreCase(String.valueOf(role)) || "hatter".equalsIgnoreCase(String.valueOf(role));
  }

  private static String systemRole(String accountType) {
    return switch (String.valueOf(accountType).toLowerCase()) {
      case "hatter" -> "HATTER";
      case "master" -> "MASTER";
      case "player" -> "PLAYER";
      default -> "GUEST";
    };
  }
}
