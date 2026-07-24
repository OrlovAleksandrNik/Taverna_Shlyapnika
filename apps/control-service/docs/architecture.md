# Architecture

Control Service является отдельным bounded context.

```mermaid
flowchart LR
  UI["control-frontend"] --> API["control-backend"]
  API --> DB["control-postgres"]
  API --> Mail["control-mail-dev"]
  API --> Storage["Local media/backup storage"]
  API -. disabled .-> Main["Main site backend"]
  API -. disabled .-> Bot["Telegram bot"]
  API -. future .-> Agent["Desktop Agent"]
```

Backend разделён по предметным областям: `auth`, `users`, `roles`, `permissions`, `dashboard`, `games`, `schedule`, `applications`, `services`, `masters`, `players`, `rating`, `gallery`, `stories`, `files`, `projects`, `backups`, `audit`, `notifications`, `settings`, `integration`.

Основные API namespaces:

- `/api/v1/public/**`
- `/api/v1/auth/**`
- `/api/v1/account/**`
- `/api/v1/admin/**`
- `/api/v1/internal/**`

На текущем этапе public endpoints минимальны, internal endpoints подготовлены концептуально, но интеграции выключены.
