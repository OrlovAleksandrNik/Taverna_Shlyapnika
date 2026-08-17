package by.taverna.shlyapnika.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class MasterSessionAuthenticationFilterTest {
  private final MasterSessionAuthenticationFilter filter = new MasterSessionAuthenticationFilter();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesHatterSessionAsBackendUser() throws Exception {
    var request = new MockHttpServletRequest();
    var session = request.getSession();
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", "Александр");
    session.setAttribute("taverna.master.role", "admin");

    filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
    });

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo("Александр");
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_HATTER");
  }

  @Test
  void skipsRequestWithoutMasterSession() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
    });

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
