package by.taverna.shlyapnika.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class MasterSessionAuthenticationFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    var session = request.getSession(false);
    if (session == null || !Boolean.TRUE.equals(session.getAttribute("taverna.master.accessGranted"))) {
      filterChain.doFilter(request, response);
      return;
    }

    var role = String.valueOf(session.getAttribute("taverna.master.role"));
    var baseRole = String.valueOf(session.getAttribute("taverna.master.baseRole"));
    var displayName = String.valueOf(session.getAttribute("taverna.master.displayName"));
    SecurityContextHolder.getContext().setAuthentication(new MasterSessionAuthentication(displayName, role, baseRole));
    filterChain.doFilter(request, response);
  }

  private static final class MasterSessionAuthentication extends AbstractAuthenticationToken {
    private final String principal;

    private MasterSessionAuthentication(String principal, String role, String baseRole) {
      super(authorities(role, baseRole));
      this.principal = principal;
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
      return "";
    }

    @Override
    public Object getPrincipal() {
      return principal;
    }

    private static List<SimpleGrantedAuthority> authorities(String role, String baseRole) {
      var authorities = new ArrayList<SimpleGrantedAuthority>();
      var normalized = String.valueOf(role).equalsIgnoreCase("admin") || String.valueOf(role).equalsIgnoreCase("hatter")
          ? "HATTER"
          : "MASTER";
      authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
      if (String.valueOf(baseRole).equalsIgnoreCase("admin") || String.valueOf(baseRole).equalsIgnoreCase("hatter")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_HATTER"));
      }
      return authorities;
    }
  }
}
