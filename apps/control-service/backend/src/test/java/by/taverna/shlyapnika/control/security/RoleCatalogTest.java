package by.taverna.shlyapnika.control.security;

import by.taverna.shlyapnika.control.auth.domain.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCatalogTest {
  @Test
  void ownerReceivesAllPermissions() {
    assertThat(RoleCatalog.permissionsFor(Set.of(UserRole.OWNER)))
        .hasSize(Permission.values().length)
        .contains(Permission.USERS_INVITE, Permission.BACKUPS_RESTORE, Permission.PROJECTS_LAUNCH);
  }

  @Test
  void masterCannotManageUsers() {
    assertThat(RoleCatalog.permissionsFor(Set.of(UserRole.MASTER)))
        .contains(Permission.GAMES_READ, Permission.PROJECTS_READ)
        .doesNotContain(Permission.USERS_ASSIGN_ROLES, Permission.SETTINGS_MANAGE);
  }
}
