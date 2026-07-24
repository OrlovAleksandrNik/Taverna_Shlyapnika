package by.taverna.shlyapnika.control.common;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionPermissionPolicyTest {
  private final SectionPermissionPolicy policy = new SectionPermissionPolicy();

  @Test
  void deniesWriteWithoutSectionPermission() {
    TestingAuthenticationToken auth = new TestingAuthenticationToken("master", "n/a", List.of(new SimpleGrantedAuthority("gallery.read")));

    assertThatThrownBy(() -> policy.requireWrite("gallery", auth))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }
}
