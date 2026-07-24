package by.taverna.shlyapnika.control.security;

import by.taverna.shlyapnika.control.auth.domain.UserRole;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static by.taverna.shlyapnika.control.auth.domain.UserRole.*;
import static by.taverna.shlyapnika.control.security.Permission.*;

public final class RoleCatalog {
  private static final Map<UserRole, Set<Permission>> PERMISSIONS = new EnumMap<>(UserRole.class);

  static {
    PERMISSIONS.put(OWNER, EnumSet.allOf(Permission.class));
    PERMISSIONS.put(SUPERADMIN, EnumSet.complementOf(EnumSet.noneOf(Permission.class)));
    PERMISSIONS.put(DEVELOPER, EnumSet.of(PROJECTS_READ, PROJECTS_LAUNCH, PROJECTS_CONFIGURE, INTEGRATIONS_READ, AUDIT_READ, SETTINGS_MANAGE, BACKUPS_READ));
    PERMISSIONS.put(MANAGER, EnumSet.of(GAMES_READ, GAMES_CREATE, GAMES_UPDATE, GAMES_PUBLISH, SCHEDULE_READ, SCHEDULE_MANAGE, APPLICATIONS_MANAGE, SERVICES_MANAGE, MASTERS_MANAGE, GALLERY_READ));
    PERMISSIONS.put(MASTER, EnumSet.of(GAMES_READ, GAMES_CREATE, GAMES_UPDATE, SCHEDULE_READ, GALLERY_READ, GALLERY_CREATE, PROJECTS_READ));
    PERMISSIONS.put(CONTENT_MANAGER, EnumSet.of(GALLERY_READ, GALLERY_CREATE, GALLERY_UPDATE, GALLERY_PUBLISH, GALLERY_DELETE, FILES_MANAGE));
    PERMISSIONS.put(RATING_MANAGER, EnumSet.of(RATING_READ, RATING_MANAGE, RATING_REVERSE));
    PERMISSIONS.put(SUPPORT, EnumSet.of(GAMES_READ, SCHEDULE_READ, APPLICATIONS_MANAGE, USERS_READ));
    PERMISSIONS.put(VIEWER, EnumSet.of(GAMES_READ, SCHEDULE_READ, GALLERY_READ, RATING_READ, PROJECTS_READ, BACKUPS_READ));
  }

  private RoleCatalog() {
  }

  public static Set<Permission> permissionsFor(Set<UserRole> roles) {
    EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
    roles.forEach(role -> result.addAll(PERMISSIONS.getOrDefault(role, Set.of())));
    return result;
  }

  public static Map<UserRole, Set<Permission>> all() {
    return Map.copyOf(PERMISSIONS);
  }
}
