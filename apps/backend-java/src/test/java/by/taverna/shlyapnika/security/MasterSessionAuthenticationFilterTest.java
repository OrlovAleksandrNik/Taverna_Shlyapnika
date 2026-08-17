package by.taverna.shlyapnika.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import by.taverna.shlyapnika.account.domain.SiteAccountEntity;
import by.taverna.shlyapnika.account.infrastructure.SiteAccountRepository;
import by.taverna.shlyapnika.schedule.domain.ConsentSnapshot;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
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

  @Test
  void invalidatesSessionWhenAccountAccessWasRevoked() throws Exception {
    var repository = mock(SiteAccountRepository.class);
    var account = SiteAccountEntity.create(
        "acc_master",
        "Мастер",
        "master@example.com",
        "hash",
        "master",
        "active",
        "@master",
        "master",
        new ConsentSnapshot("1.0", "1.0", Instant.now(), "test")
    );
    account.block();
    when(repository.findById("acc_master")).thenReturn(Optional.of(account));
    var provider = new ObjectProvider<SiteAccountRepository>() {
      @Override public SiteAccountRepository getObject(Object... args) { return repository; }
      @Override public SiteAccountRepository getIfAvailable() { return repository; }
      @Override public SiteAccountRepository getIfUnique() { return repository; }
      @Override public SiteAccountRepository getObject() { return repository; }
    };
    var securedFilter = new MasterSessionAuthenticationFilter(provider);
    var request = new MockHttpServletRequest();
    var session = request.getSession();
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", "Мастер");
    session.setAttribute("taverna.master.role", "master");
    session.setAttribute("taverna.auth.accountId", "acc_master");

    securedFilter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
    });

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(((MockHttpSession) session).isInvalid()).isTrue();
  }
}
