package by.taverna.shlyapnika.control.security;

public enum Permission {
  GAMES_READ("games.read"),
  GAMES_CREATE("games.create"),
  GAMES_UPDATE("games.update"),
  GAMES_DELETE("games.delete"),
  GAMES_PUBLISH("games.publish"),
  SCHEDULE_READ("schedule.read"),
  SCHEDULE_MANAGE("schedule.manage"),
  GALLERY_READ("gallery.read"),
  GALLERY_CREATE("gallery.create"),
  GALLERY_UPDATE("gallery.update"),
  GALLERY_PUBLISH("gallery.publish"),
  GALLERY_DELETE("gallery.delete"),
  RATING_READ("rating.read"),
  RATING_MANAGE("rating.manage"),
  RATING_REVERSE("rating.reverse"),
  USERS_READ("users.read"),
  USERS_INVITE("users.invite"),
  USERS_UPDATE("users.update"),
  USERS_BLOCK("users.block"),
  USERS_ASSIGN_ROLES("users.assign_roles"),
  PROJECTS_READ("projects.read"),
  PROJECTS_LAUNCH("projects.launch"),
  PROJECTS_CONFIGURE("projects.configure"),
  BACKUPS_CREATE("backups.create"),
  BACKUPS_RESTORE("backups.restore"),
  BACKUPS_READ("backups.read"),
  AUDIT_READ("audit.read"),
  SETTINGS_MANAGE("settings.manage"),
  FILES_MANAGE("files.manage"),
  APPLICATIONS_MANAGE("applications.manage"),
  SERVICES_MANAGE("services.manage"),
  MASTERS_MANAGE("masters.manage"),
  NOTIFICATIONS_MANAGE("notifications.manage"),
  INTEGRATIONS_READ("integrations.read");

  private final String value;

  Permission(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
