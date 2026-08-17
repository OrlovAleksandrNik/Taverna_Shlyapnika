package by.taverna.shlyapnika.security;

import by.taverna.shlyapnika.account.domain.SiteAccountEntity;
import by.taverna.shlyapnika.account.infrastructure.SiteAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MasterSessionAuthenticationFilter extends OncePerRequestFilter {
  private final ObjectProvider<SiteAccountRepository> accountsProvider;

  public MasterSessionAuthenticationFilter() {
    this.accountsProvider = new EmptyAccountsProvider();
  }

  public MasterSessionAuthenticationFilter(ObjectProvider<SiteAccountRepository> accountsProvider) {
    this.accountsProvider = accountsProvider;
  }

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

    var account = currentAccount(session.getAttribute("taverna.auth.accountId"));
    if (account != null && !sessionStillAllowed(session, account)) {
      session.invalidate();
      filterChain.doFilter(request, response);
      return;
    }
    if (account != null) refreshSession(session, account);

    var role = String.valueOf(session.getAttribute("taverna.master.role"));
    var baseRole = String.valueOf(session.getAttribute("taverna.master.baseRole"));
    var displayName = String.valueOf(session.getAttribute("taverna.master.displayName"));
    SecurityContextHolder.getContext().setAuthentication(new MasterSessionAuthentication(displayName, role, baseRole));
    filterChain.doFilter(request, response);
  }

  private SiteAccountEntity currentAccount(Object accountId) {
    if (accountId == null) return null;
    var accounts = accountsProvider.getIfAvailable();
    if (accounts == null) return null;
    return accounts.findById(String.valueOf(accountId)).orElse(null);
  }

  private boolean sessionStillAllowed(jakarta.servlet.http.HttpSession session, SiteAccountEntity account) {
    var sessionVersion = session.getAttribute("taverna.auth.sessionVersion");
    var versionMatches = sessionVersion == null || String.valueOf(sessionVersion).equals(String.valueOf(account.getSessionVersion()));
    return "active".equals(account.getStatus()) && versionMatches;
  }

  private void refreshSession(jakarta.servlet.http.HttpSession session, SiteAccountEntity account) {
    var baseRole = legacyRole(account.getRole());
    var canSwitch = "admin".equalsIgnoreCase(baseRole);
    var requestedProfileMode = stringAttribute(session, "taverna.master.profileMode");
    var profileMode = canSwitch && "master".equalsIgnoreCase(requestedProfileMode) ? "master" : (canSwitch ? "hatter" : account.getRole());
    var role = canSwitch && "master".equals(profileMode) ? "master" : baseRole;
    var displayName = canSwitch && "master".equals(profileMode) ? "Мастер Александр" : (canSwitch ? "Шляпник" : account.getDisplayName());
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", displayName);
    session.setAttribute("taverna.master.role", role);
    session.setAttribute("taverna.master.baseRole", baseRole);
    session.setAttribute("taverna.master.profileMode", profileMode);
    session.setAttribute("taverna.master.telegramUsername", account.getTelegramUsername());
    session.setAttribute("taverna.master.email", account.getEmail());
    session.setAttribute("taverna.auth.accountType", account.getRole());
    session.setAttribute("taverna.auth.status", account.getStatus());
    session.setAttribute("taverna.auth.systemRole", systemRole(account.getRole()));
    session.setAttribute("taverna.auth.activeProfile", canSwitch && "master".equals(profileMode) ? "alexander" : profileMode);
    session.setAttribute("taverna.auth.sessionVersion", account.getSessionVersion());
  }

  private static String stringAttribute(jakarta.servlet.http.HttpSession session, String key) {
    var value = session.getAttribute(key);
    return value == null ? null : String.valueOf(value);
  }

  private static String legacyRole(String role) {
    return "hatter".equals(role) ? "admin" : role;
  }

  private static String systemRole(String role) {
    return switch (String.valueOf(role)) {
      case "hatter" -> "HATTER";
      case "master" -> "MASTER";
      case "player" -> "PLAYER";
      default -> "GUEST";
    };
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
      var normalized = normalizeRole(role);
      authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
      if (String.valueOf(baseRole).equalsIgnoreCase("admin") || String.valueOf(baseRole).equalsIgnoreCase("hatter")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_HATTER"));
      }
      return authorities;
    }

    private static String normalizeRole(String role) {
      var value = String.valueOf(role);
      if (value.equalsIgnoreCase("admin") || value.equalsIgnoreCase("hatter")) return "HATTER";
      if (value.equalsIgnoreCase("player")) return "PLAYER";
      return "MASTER";
    }
  }

  private static final class EmptyAccountsProvider implements ObjectProvider<SiteAccountRepository> {
    @Override
    public SiteAccountRepository getObject(Object... args) { return null; }
    @Override
    public SiteAccountRepository getIfAvailable() { return null; }
    @Override
    public SiteAccountRepository getIfUnique() { return null; }
    @Override
    public SiteAccountRepository getObject() { return null; }
  }
}
