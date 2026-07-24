# Roles And Permissions

Роли:

- OWNER
- SUPERADMIN
- DEVELOPER
- MANAGER
- MASTER
- CONTENT_MANAGER
- RATING_MANAGER
- SUPPORT
- VIEWER

Permissions проверяются backend через `@PreAuthorize`, а не только frontend-навигацией.

Ключевые permissions:

- `games.read`, `games.create`, `games.update`, `games.delete`, `games.publish`
- `schedule.read`, `schedule.manage`
- `gallery.read`, `gallery.create`, `gallery.update`, `gallery.publish`, `gallery.delete`
- `rating.read`, `rating.manage`, `rating.reverse`
- `users.read`, `users.invite`, `users.update`, `users.block`, `users.assign_roles`
- `projects.read`, `projects.launch`, `projects.configure`
- `backups.read`, `backups.create`, `backups.restore`
- `audit.read`, `settings.manage`

Последнего OWNER нельзя удалять: это правило должно применяться в user-management mutations.
