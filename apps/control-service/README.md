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
- users: invitation creation through `/api/v1/admin/users/invitations`;
- games: create draft games through `/api/v1/admin/games`;
- games: publish, cancel and soft-delete existing backend games;
- generic records: create, publish and archive draft records through `/api/v1/admin/data/{section}`;
- projects: mock launch through `/api/v1/admin/projects/{code}/launch`;
- backups: create metadata-only backup manifest and call restore-blocked contract.
- settings/tech: read feature flags and isolated integration status from backend when authenticated.

Known local blocker in this workspace: `java` and `mvn` are not available in PATH, so backend build/tests require JDK 21 plus Maven or the Maven wrapper with Java installed.
