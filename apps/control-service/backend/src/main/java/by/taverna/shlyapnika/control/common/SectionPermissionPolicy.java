package by.taverna.shlyapnika.control.common;

import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SectionPermissionPolicy {
  private static final Map<String, SectionPermissions> SECTIONS = Map.ofEntries(
      Map.entry("applications", new SectionPermissions("applications.manage", "applications.manage", "applications.manage")),
      Map.entry("services", new SectionPermissions("services.manage", "services.manage", "services.manage")),
      Map.entry("masters", new SectionPermissions("masters.manage", "masters.manage", "masters.manage")),
      Map.entry("players", new SectionPermissions("rating.read", "rating.manage", "rating.manage")),
      Map.entry("rating", new SectionPermissions("rating.read", "rating.manage", "rating.reverse")),
      Map.entry("gallery", new SectionPermissions("gallery.read", "gallery.create", "gallery.publish")),
      Map.entry("stories", new SectionPermissions("gallery.read", "gallery.create", "gallery.publish")),
      Map.entry("notifications", new SectionPermissions("notifications.manage", "notifications.manage", "notifications.manage"))
  );

  public SectionPermissions permissionsFor(String section) {
    SectionPermissions permissions = SECTIONS.get(section);
    if (permissions == null) {
      throw new IllegalArgumentException("Unsupported section.");
    }
    return permissions;
  }

  public void requireRead(String section, Authentication authentication) {
    require(permissionsFor(section).read(), authentication);
  }

  public void requireWrite(String section, Authentication authentication) {
    require(permissionsFor(section).write(), authentication);
  }

  public void requirePublish(String section, Authentication authentication) {
    require(permissionsFor(section).publish(), authentication);
  }

  private static void require(String permission, Authentication authentication) {
    Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
    if (!authorities.contains(permission)) {
      throw new org.springframework.security.access.AccessDeniedException("Missing permission: " + permission);
    }
  }

  public record SectionPermissions(String read, String write, String publish) {
  }
}
