# Development Plan

Реализованный сейчас объём:

- audit существующих проектов;
- isolated backend/frontend skeleton;
- роли и granular permissions;
- invitation-based account flow;
- games CRUD with optimistic locking field and soft delete;
- schedule read contract;
- dashboard mock data;
- managed projects mock contracts;
- backup manifest contract;
- audit log;
- security headers, CORS и CSRF;
- Docker Compose;
- documentation.

Следующие production-hardening задачи:

- полноценный TOTP с QR, encrypted secret и hashed backup codes;
- password reset/email verification mail flows;
- persistent active-device management;
- real table pagination/filter persistence;
- Testcontainers PostgreSQL integration suite;
- e2e через Playwright после установки frontend dev dependencies;
- Desktop Agent для VoiceMod/ScreenStage.
