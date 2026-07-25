package by.taverna.shlyapnika.characters.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiTokenFilter extends OncePerRequestFilter {
  private final CharacterServiceProperties properties;

  public ApiTokenFilter(CharacterServiceProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!request.getRequestURI().startsWith("/api/") || !properties.hasApiToken() || tokenMatches(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"message\":\"Character API token is required.\"}");
  }

  private boolean tokenMatches(HttpServletRequest request) {
    var candidate = request.getHeader("X-Character-Service-Token");
    var authorization = request.getHeader("Authorization");
    if ((candidate == null || candidate.isBlank()) && authorization != null && authorization.startsWith("Bearer ")) {
      candidate = authorization.substring("Bearer ".length());
    }
    if (candidate == null || candidate.isBlank()) return false;

    return MessageDigest.isEqual(
        candidate.getBytes(StandardCharsets.UTF_8),
        properties.apiToken().getBytes(StandardCharsets.UTF_8));
  }
}
