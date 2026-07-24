# Taverna Control в Railway

`apps/control-service` разворачивается как отдельный административный микросервис в том же Railway project, где живёт основной сайт, но без подключения к сайту, его backend или production database.

## Безопасная схема

```text
Railway project "Taverna Shlyapnika"
├── main-site                 # существующий публичный сайт, не менять
├── main-site-postgres        # существующая production DB сайта, не использовать
├── control-backend           # новый service, root apps/control-service/backend
├── control-frontend          # новый service, root apps/control-service/frontend
└── control-postgres          # новый PostgreSQL только для control-backend
```

Запрещено на этом этапе:

- подключать `control-backend` к `DATABASE_URL` основного сайта;
- добавлять ссылки на кабинет в публичный сайт;
- включать `CONTROL_MAIN_SITE_INTEGRATION_ENABLED`;
- включать `CONTROL_TELEGRAM_INTEGRATION_ENABLED`;
- передавать путь запуска программ из браузера.

## Railway services

Создайте в том же Railway project два новых GitHub services из этого репозитория:

| Service | Root directory | Railway config | Назначение |
| --- | --- | --- | --- |
| `control-backend` | `apps/control-service/backend` | `railway.json` | Spring Boot API, Flyway, isolated DB |
| `control-frontend` | `apps/control-service/frontend` | `railway.json` | Nginx static admin UI |

Добавьте отдельный Railway PostgreSQL service, например `ControlPostgres`, и подключайте только его variables к `control-backend`.

## Variables: control-backend

Минимальный набор:

```env
PORT=4190
CONTROL_DATABASE_URL=jdbc:postgresql://${{ControlPostgres.PGHOST}}:${{ControlPostgres.PGPORT}}/${{ControlPostgres.PGDATABASE}}
CONTROL_DATABASE_USERNAME=${{ControlPostgres.PGUSER}}
CONTROL_DATABASE_PASSWORD=${{ControlPostgres.PGPASSWORD}}
CONTROL_FRONTEND_ORIGIN=https://<control-frontend>.up.railway.app
CONTROL_PUBLIC_URL=https://<control-frontend>.up.railway.app
CONTROL_SESSION_SECRET=<generate-64-char-secret>
CONTROL_ENCRYPTION_KEY=<generate-32-byte-minimum-secret>
CONTROL_COOKIE_SECURE=true
PUBLIC_REGISTRATION_ENABLED=false
CONTROL_MAIN_SITE_INTEGRATION_ENABLED=false
CONTROL_TELEGRAM_INTEGRATION_ENABLED=false
CONTROL_DESKTOP_AGENT_ENABLED=false
CONTROL_MAIL_PROVIDER=mock
CONTROL_BOOTSTRAP_OWNER_EMAIL=<owner-email>
CONTROL_BOOTSTRAP_TOKEN=<one-time-strong-password>
```

`CONTROL_BOOTSTRAP_TOKEN` используется как первичный пароль только до создания первого OWNER. После первого входа смените пароль и уберите bootstrap variables из Railway.

## Variables: control-frontend

```env
CONTROL_API_BASE=https://<control-backend>.up.railway.app
```

Frontend получает это значение на runtime через `/control-runtime-config.js`; пересборка образа для смены backend URL не нужна.

## Проверка после deploy

1. Откройте `https://<control-backend>.up.railway.app/health`.
2. Откройте `https://<control-frontend>.up.railway.app/health`.
3. Откройте frontend URL, выберите раздел `Безопасность`.
4. Войдите bootstrap OWNER.
5. Создайте отдельного пользователя через invitation.
6. Смените пароль OWNER и удалите `CONTROL_BOOTSTRAP_OWNER_EMAIL` / `CONTROL_BOOTSTRAP_TOKEN` из Railway variables.

## Что остаётся изолированным

- DB schema: `control_*` и `SPRING_SESSION*` в отдельной control database.
- API namespace: `/api/v1/**`, не совпадает с публичным сайтом.
- CORS: только `CONTROL_FRONTEND_ORIGIN`.
- Cookies: `CONTROLSESSION`, не используется сайтом.
- Desktop launch: только allowlist env, по умолчанию выключен.
