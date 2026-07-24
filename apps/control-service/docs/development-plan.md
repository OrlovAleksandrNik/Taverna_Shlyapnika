# Development Plan

Реализованный сейчас объём:

- audit существующих проектов;
- isolated backend/frontend skeleton;
- роли и granular permissions;
- invitation-based account flow;
- password reset and email verification hashed token flows;
- TOTP setup, encrypted TOTP secret and hashed backup codes;
- account session metadata and revoke-all contract;
- in-memory login throttling contract;
- games CRUD with optimistic locking field and soft delete;
- schedule read contract;
- dashboard mock data;
- managed projects mock contracts;
- backup manifest contract;
- local media/project artifact storage contracts;
- file validation policy that blocks executable upload formats;
- generic control records CRUD API with pagination, search, optimistic locking, publish and soft-delete;
- frontend table controls for search, filters, sorting, selection, bulk actions, columns and export;
- backend-aware frontend data loading with mock fallback and source badge;
- frontend actions for login, session check, password reset, user invitations, game creation/lifecycle, generic record creation/lifecycle, project mock launch and backup contracts;
- frontend settings and tech screens backed by feature-flag and integration-status endpoints;
- frontend action contract test for endpoint and CSRF wiring;
- frontend TOTP setup controls with bundled scannable QR canvas rendering, confirm/disable actions and session revoke actions;
- persisted table search/status/sort/page/column preferences in `localStorage`;
- Playwright e2e suite for desktop and mobile preview checks;
- Flyway mock seed data for management sections;
- Flyway Spring Session JDBC schema migration;
- SMTP mail provider behind `CONTROL_MAIL_PROVIDER=smtp`;
- persistent revoked-session enforcement for known `CONTROLSESSION` ids;
- allowlisted Desktop Agent launch contract for VoiceMod and ScreenStage;
- Testcontainers PostgreSQL migration validation suite;
- frontend role-based navigation smoke checks;
- audit log;
- security headers, CORS и CSRF;
- Docker Compose;
- documentation.

Следующие production-hardening задачи:

- добавить визуальный download/copy flow для QR/secret и recovery codes перед реальным rollout 2FA;
- подключить реальные SMTP credentials и домен отправителя;
- установить Docker Desktop или локальный PostgreSQL для полного `docker compose up --build`;
- запустить Testcontainers suite на машине с Docker daemon;
- добавить service-to-service authentication/checksum layer для полноценного Desktop Agent.
