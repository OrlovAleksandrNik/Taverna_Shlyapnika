package by.taverna.shlyapnika.control.users.api;

import by.taverna.shlyapnika.control.auth.api.AuthController;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.AccountResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.InvitationResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.InviteRequest;
import by.taverna.shlyapnika.control.auth.application.AuthService;
import by.taverna.shlyapnika.control.auth.infrastructure.UserAccountRepository;
import by.taverna.shlyapnika.control.security.ControlUserDetailsService.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {
  private final UserAccountRepository users;
  private final AuthService auth;

  public UserAdminController(UserAccountRepository users, AuthService auth) {
    this.users = users;
    this.auth = auth;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('users.read')")
  List<AccountResponse> list() {
    return users.findAll().stream().map(AuthController::toResponse).toList();
  }

  @PostMapping("/invitations")
  @PreAuthorize("hasAuthority('users.invite')")
  InvitationResponse invite(@Valid @RequestBody InviteRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    Principal principal = (Principal) authentication.getPrincipal();
    AuthService.InvitationResult result = auth.createInvitation(principal.user(), request.email(), request.displayName(), request.role(), servletRequest.getRemoteAddr());
    return new InvitationResponse(result.id(), result.oneTimeToken(), result.expiresAt());
  }
}
