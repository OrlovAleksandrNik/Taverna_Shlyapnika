package by.taverna.shlyapnika.access.api;

import by.taverna.shlyapnika.access.MasterAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
      session.setAttribute("taverna.master.telegramUsername", request.telegramUsername());
      session.setAttribute("taverna.master.email", request.email());
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
    return new MasterSessionResponse(
        true,
        stringAttribute(session, "taverna.master.displayName"),
        stringAttribute(session, "taverna.master.role"),
        stringAttribute(session, "taverna.master.telegramUsername"),
        stringAttribute(session, "taverna.master.email")
    );
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
}
