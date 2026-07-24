package by.taverna.shlyapnika.control.users.api;

import by.taverna.shlyapnika.control.auth.api.AuthController;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.AccountResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.InvitationResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.InviteRequest;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.RoleUpdateRequest;
import by.taverna.shlyapnika.control.auth.application.AuthService;
import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.auth.domain.UserRole;
import by.taverna.shlyapnika.control.auth.infrastructure.UserAccountRepository;
import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.security.ControlUserDetailsService.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {
  private final UserAccountRepository users;
  private final AuthService auth;
  private final AuditService audit;

  public UserAdminController(UserAccountRepository users, AuthService auth, AuditService audit) {
    this.users = users;
    this.auth = auth;
    this.audit = audit;
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

  @PutMapping("/{id}/roles")
  @PreAuthorize("hasAuthority('users.assign_roles')")
  AccountResponse updateRoles(@PathVariable UUID id, @Valid @RequestBody RoleUpdateRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    UserAccount target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found."));
    Set<UserRole> nextRoles = request.roles() == null || request.roles().isEmpty() ? Set.of(UserRole.VIEWER) : request.roles();
    if (target.getRoles().contains(UserRole.OWNER) && !nextRoles.contains(UserRole.OWNER) && users.countByRole(UserRole.OWNER) <= 1) {
      throw new IllegalArgumentException("The last OWNER role cannot be removed.");
    }
    target.updateRoles(nextRoles);
    audit.record(authentication.getName(), "users.assign_roles", "UserAccount", target.getPublicId(), "roles=" + nextRoles, servletRequest.getRemoteAddr());
    return AuthController.toResponse(users.save(target));
  }

  @PostMapping("/{id}/block")
  @PreAuthorize("hasAuthority('users.block')")
  AccountResponse block(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    UserAccount target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found."));
    if (target.getRoles().contains(UserRole.OWNER) && users.countByRole(UserRole.OWNER) <= 1) {
      throw new IllegalArgumentException("The last OWNER cannot be blocked.");
    }
    target.block();
    audit.record(authentication.getName(), "users.block", "UserAccount", target.getPublicId(), "status=BLOCKED", servletRequest.getRemoteAddr());
    return AuthController.toResponse(users.save(target));
  }

  @PostMapping("/{id}/unblock")
  @PreAuthorize("hasAuthority('users.block')")
  AccountResponse unblock(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    UserAccount target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found."));
    target.unblock();
    audit.record(authentication.getName(), "users.unblock", "UserAccount", target.getPublicId(), "status=ACTIVE", servletRequest.getRemoteAddr());
    return AuthController.toResponse(users.save(target));
  }

  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasAuthority('users.update')")
  AccountResponse deactivate(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    UserAccount target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found."));
    if (target.getRoles().contains(UserRole.OWNER) && users.countByRole(UserRole.OWNER) <= 1) {
      throw new IllegalArgumentException("The last OWNER cannot be deactivated.");
    }
    target.deactivate();
    audit.record(authentication.getName(), "users.deactivate", "UserAccount", target.getPublicId(), "status=DEACTIVATED", servletRequest.getRemoteAddr());
    return AuthController.toResponse(users.save(target));
  }

  @PostMapping("/{id}/delete")
  @PreAuthorize("hasAuthority('users.update')")
  void softDelete(@PathVariable UUID id, Authentication authentication, HttpServletRequest servletRequest) {
    UserAccount target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found."));
    if (target.getRoles().contains(UserRole.OWNER) && users.countByRole(UserRole.OWNER) <= 1) {
      throw new IllegalArgumentException("The last OWNER cannot be deleted.");
    }
    target.softDelete();
    users.save(target);
    audit.record(authentication.getName(), "users.soft_delete", "UserAccount", target.getPublicId(), "status=DELETED", servletRequest.getRemoteAddr());
  }
}
