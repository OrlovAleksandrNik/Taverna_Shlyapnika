package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.auth.infrastructure.ControlSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ControlSessionValidationFilter extends OncePerRequestFilter {
  private final ControlSessionRepository sessions;

  public ControlSessionValidationFilter(ControlSessionRepository sessions) {
    this.sessions = sessions;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    HttpSession httpSession = request.getSession(false);
    if (httpSession != null) {
      boolean revokedOrExpired = sessions.findBySessionHash(AuthService.sha256(httpSession.getId()))
          .map(session -> !session.isActive(Instant.now()))
          .orElse(false);
      if (revokedOrExpired) {
        SecurityContextHolder.clearContext();
        httpSession.invalidate();
        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session was revoked.");
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
