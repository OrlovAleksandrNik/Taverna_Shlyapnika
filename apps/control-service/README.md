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
