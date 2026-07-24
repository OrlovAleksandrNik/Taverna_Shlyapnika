package by.taverna.shlyapnika.control.auth.api;

import by.taverna.shlyapnika.control.auth.api.AuthDtos.AcceptInvitationRequest;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.AccountResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.InvitationResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.LoginRequest;
import by.taverna.shlyapnika.control.auth.application.AuthService;
import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.security.ControlUserDetailsService.Principal;
import by.taverna.shlyapnika.control.security.RoleCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/csrf")
  Map<String, Boolean> csrf() {
    return Map.of("csrfCookieIssued", true);
  }

  @PostMapping("/login")
  ResponseEntity<AccountResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    UserAccount account = authService.authenticate(
        request.email(),
        request.password(),
        servletRequest.getRemoteAddr(),
        servletRequest.getHeader("User-Agent"));
    Principal principal = new Principal(account);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    HttpSession session = servletRequest.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return ResponseEntity.ok(toResponse(account));
  }

  @PostMapping("/invitations/accept")
  ResponseEntity<AccountResponse> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
    return ResponseEntity.ok(toResponse(authService.acceptInvitation(request.token(), request.password())));
  }

  @GetMapping("/me")
  AccountResponse me(org.springframework.security.core.Authentication authentication) {
    return toResponse(((Principal) authentication.getPrincipal()).user());
  }

  public static AccountResponse toResponse(UserAccount account) {
    return new AccountResponse(
        account.getPublicId(),
        account.getDisplayName(),
        account.getEmail(),
        account.getRoles(),
        RoleCatalog.permissionsFor(account.getRoles()).stream().map(permission -> permission.value()).collect(java.util.stream.Collectors.toSet()),
        account.isTwoFactorEnabled(),
        account.getLastLoginAt());
  }
}
