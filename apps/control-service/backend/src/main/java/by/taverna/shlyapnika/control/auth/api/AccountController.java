package by.taverna.shlyapnika.control.auth.api;

import by.taverna.shlyapnika.control.auth.api.AuthDtos.SessionResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.TokenResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.TwoFactorBackupCodesResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.TwoFactorDisableRequest;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.TwoFactorSetupResponse;
import by.taverna.shlyapnika.control.auth.api.AuthDtos.TwoFactorVerifyRequest;
import by.taverna.shlyapnika.control.auth.application.AuthService;
import by.taverna.shlyapnika.control.auth.domain.ControlSession;
import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.security.ControlUserDetailsService.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
  private final AuthService authService;

  public AccountController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/email-verification")
  TokenResponse requestEmailVerification(Authentication authentication, HttpServletRequest servletRequest) {
    AuthService.TokenResult result = authService.requestEmailVerification(user(authentication), servletRequest.getRemoteAddr());
    return new TokenResponse(result.issued(), result.devOnlyToken());
  }

  @PostMapping("/email-verification/confirm")
  Map<String, Boolean> confirmEmailVerification(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
    authService.verifyEmail(request.get("token"), servletRequest.getRemoteAddr());
    return Map.of("emailVerified", true);
  }

  @PostMapping("/2fa/setup")
  TwoFactorSetupResponse startTwoFactor(Authentication authentication, HttpServletRequest servletRequest) {
    AuthService.TwoFactorSetup setup = authService.startTwoFactorSetup(user(authentication), servletRequest.getRemoteAddr());
    return new TwoFactorSetupResponse(setup.secret(), setup.otpauthUrl());
  }

  @PostMapping("/2fa/confirm")
  TwoFactorBackupCodesResponse confirmTwoFactor(
      @Valid @RequestBody TwoFactorVerifyRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    List<String> codes = authService.confirmTwoFactor(user(authentication), request.code(), servletRequest.getRemoteAddr());
    return new TwoFactorBackupCodesResponse(Set.copyOf(codes));
  }

  @PostMapping("/2fa/disable")
  Map<String, Boolean> disableTwoFactor(
      @Valid @RequestBody TwoFactorDisableRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    authService.disableTwoFactor(user(authentication), request.password(), request.code(), servletRequest.getRemoteAddr());
    return Map.of("twoFactorEnabled", false);
  }

  @GetMapping("/sessions")
  List<SessionResponse> sessions(Authentication authentication) {
    return authService.sessionsFor(user(authentication)).stream().map(AccountController::toResponse).toList();
  }

  @PostMapping("/sessions/revoke-all")
  Map<String, Boolean> revokeSessions(Authentication authentication, HttpServletRequest servletRequest) {
    authService.revokeAllSessions(user(authentication), servletRequest.getRemoteAddr());
    if (servletRequest.getSession(false) != null) {
      servletRequest.getSession(false).invalidate();
    }
    return Map.of("sessionsRevoked", true);
  }

  private static UserAccount user(Authentication authentication) {
    return ((Principal) authentication.getPrincipal()).user();
  }

  private static SessionResponse toResponse(ControlSession session) {
    return new SessionResponse(
        session.getId().toString(),
        session.getUserAgent(),
        session.getIpAddress(),
        session.getCreatedAt(),
        session.getExpiresAt(),
        session.getRevokedAt());
  }
}
