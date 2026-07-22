package by.taverna.shlyapnika.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicPostRateLimitFilterTest {
  @Test
  void limitsRepeatedPublicPostRequests() throws ServletException, IOException {
    var filter = new PublicPostRateLimitFilter();
    MockHttpServletResponse response = null;

    for (var index = 0; index < 13; index++) {
      var request = new MockHttpServletRequest("POST", "/api/game-signups");
      request.setRemoteAddr("203.0.113.10");
      request.addHeader("user-agent", "test-agent");
      response = new MockHttpServletResponse();
      filter.doFilter(request, response, new MockFilterChain());
    }

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getContentAsString()).contains("RATE_LIMITED");
  }

  @Test
  void ignoresPublicGetRequests() throws ServletException, IOException {
    var request = new MockHttpServletRequest("GET", "/api/games");
    var response = new MockHttpServletResponse();

    new PublicPostRateLimitFilter().doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(200);
  }
}
