# Backups

Backup storage abstraction:

- `BackupStorage`
- `LocalBackupStorage`
- future S3-compatible adapter

На текущем этапе backend создаёт metadata-only manifest в `CONTROL_BACKUP_STORAGE_ROOT`. Это не production backup основного сайта.

Restore:

- endpoint присутствует как guarded contract;
- восстановление выключено;
- для включения нужен runbook, double confirmation и audit.

Проверка:

- manifest получает SHA-256 checksum;
- запись сохраняется в `control_backup_jobs`;
- операция пишется в audit log.
