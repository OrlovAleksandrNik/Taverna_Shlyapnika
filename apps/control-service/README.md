# Taverna Control

Изолированный административный микросервис для кабинета «Таверны Шляпника».

## Структура

- `backend/` — Java 21, Spring Boot 3, JPA, Flyway, Security, Actuator, OpenAPI.
- `frontend/` — отдельный административный frontend с mock-данными и технической страницей.
- `docs/` — архитектура, безопасность, роли, backup и будущая интеграция.
- `docker-compose.yml` — изолированные `control-frontend`, `control-backend`, `control-postgres`, `control-mail-dev`.

## Границы

Сервис не подключён к основному сайту, Java backend, Telegram bot или production database. Интеграционные флаги выключены в `.env.example`.

## Локальный запуск

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Frontend: `http://localhost:4191`.
Backend: `http://localhost:4190`.
Mail dev UI: `http://localhost:4192`.

## Railway

Для безопасного deploy в том же Railway project, где живёт сайт, используйте отдельные ресурсы Control:

- `control-frontend` с root directory `apps/control-service` как combined UI + API service;
- отдельный PostgreSQL service `Postgres-jr3Q`.

Не используйте production `DATABASE_URL` сайта. Подробный гид и список variables: `../../docs/control-service-railway.md`.

Первичный OWNER создаётся только при заданных `CONTROL_BOOTSTRAP_OWNER_EMAIL` и `CONTROL_BOOTSTRAP_TOKEN`. Не используйте `admin/admin`.

## Preview without backend

```powershell
cd apps/control-service/frontend
node scripts/static-server.mjs
```

Preview: `http://localhost:4191`. Backend status can be `offline` when Java backend is not running.

## Current UI contracts

The static preview is backend-aware. It tries `http://localhost:4190` first and falls back to mock data when the backend is offline or the current browser has no authenticated `CONTROLSESSION`.

Implemented frontend actions:

- auth: login, current session check, password reset request;
- account security: TOTP setup/confirm/disable, bundled scannable QR canvas, sessions list and revoke-all;
- users: invitation creation through `/api/v1/admin/users/invitations`;
- games: create draft games through `/api/v1/admin/games`;
- games: publish, cancel and soft-delete existing backend games;
- generic records: create, publish and archive draft records through `/api/v1/admin/data/{section}`;
- projects: mock launch through `/api/v1/admin/projects/{code}/launch`;
- backups: create metadata-only backup manifest and call restore-blocked contract.
- settings/tech: read feature flags and isolated integration status from backend when authenticated.
- tables: search, status filter, sort, page and compact columns are persisted in `localStorage`.

Production-oriented toggles:

- `CONTROL_MAIL_PROVIDER=mock|smtp`; SMTP uses `CONTROL_MAIL_HOST`, `CONTROL_MAIL_PORT`, `CONTROL_MAIL_USERNAME`, `CONTROL_MAIL_PASSWORD`, `CONTROL_MAIL_FROM`, `CONTROL_MAIL_START_TLS`.
- Spring Session JDBC schema is managed by Flyway migration `V3__spring_session_jdbc.sql`.
- `CONTROL_DESKTOP_AGENT_ENABLED=true` only launches allowlisted project paths from `CONTROL_DESKTOP_AGENT_VOICEMOD_PATH` and `CONTROL_DESKTOP_AGENT_SCREENSTAGE_PATH`; browser-supplied paths are never accepted.

Local backend checks use JDK 21 and the bundled Maven Wrapper:

```powershell
cd apps/control-service/backend
.\mvnw.cmd test
```

In the current Codex shell, set `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` before running Maven if the process was opened before JDK installation.

Integration tests with PostgreSQL/Testcontainers:

```powershell
cd apps/control-service/backend
.\mvnw.cmd verify -Dskip.integration.tests=false
```

Without Docker daemon the PostgreSQL integration test is skipped cleanly.

Frontend checks:

```powershell
cd apps/control-service/frontend
pnpm install --no-frozen-lockfile
node scripts/frontend-smoke-test.mjs
node scripts/role-navigation-test.mjs
node scripts/action-contract-test.mjs
.\node_modules\.bin\playwright.cmd test
```

Local H2 backend preview, useful when Docker/PostgreSQL are not installed:

```powershell
cd apps/control-service/backend
.\mvnw.cmd -DskipTests package
java -jar target\control-service-backend-0.1.0.jar --spring.profiles.active=test
```
