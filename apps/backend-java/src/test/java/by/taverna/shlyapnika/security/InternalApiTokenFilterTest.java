package by.taverna.shlyapnika.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class InternalApiTokenFilterTest {
  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesInternalRequestWithValidToken() throws ServletException, IOException {
    var request = new MockHttpServletRequest("POST", "/api/internal/archive-past-games");
    request.addHeader("x-internal-token", "secret-token");
    var response = new MockHttpServletResponse();

    new InternalApiTokenFilter("secret-token").doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
  }

  @Test
  void doesNotAuthenticateInternalRequestWithWrongToken() throws ServletException, IOException {
    var request = new MockHttpServletRequest("POST", "/api/internal/archive-past-games");
    request.addHeader("x-internal-token", "wrong");
    var response = new MockHttpServletResponse();

    new InternalApiTokenFilter("secret-token").doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
