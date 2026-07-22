package by.taverna.shlyapnika.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalApiTokenFilter extends OncePerRequestFilter {
  private final String token;

  public InternalApiTokenFilter(String token) {
    this.token = token;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!request.getRequestURI().startsWith("/api/internal/")) {
      filterChain.doFilter(request, response);
      return;
    }

    var provided = request.getHeader("x-internal-token");
    if (token != null && token.equals(provided)) {
      var auth = new InternalAuthentication();
      SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
  }

  private static final class InternalAuthentication extends AbstractAuthenticationToken {
    private InternalAuthentication() {
      super(List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
      return "";
    }

    @Override
    public Object getPrincipal() {
      return "internal-service";
    }
  }
}
