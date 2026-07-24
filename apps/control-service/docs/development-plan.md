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
- frontend actions for login, session check, password reset, user invitations, generic record creation, project mock launch and backup contracts;
- Flyway mock seed data for management sections;
- frontend role-based navigation smoke checks;
- audit log;
- security headers, CORS и CSRF;
- Docker Compose;
- documentation.

Следующие production-hardening задачи:

- QR rendering on frontend for TOTP setup;
- production mail provider;
- persistent active-device invalidation with Spring Session;
- real table pagination/filter persistence;
- Testcontainers PostgreSQL integration suite;
- e2e через Playwright после установки frontend dev dependencies;
- Desktop Agent для VoiceMod/ScreenStage.
