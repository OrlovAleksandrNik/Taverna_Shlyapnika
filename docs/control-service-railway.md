# Taverna Control в Railway

> Архивная справка. Новая рабочая схема проекта: кабинет мастера собирается и публикуется внутри основного монолита по маршруту `/master-cabinet/`. Этот документ оставлен только для понимания старой изолированной попытки.

`apps/control-service` разворачивается как отдельный административный микросервис в том же Railway project, где живёт основной сайт, но без подключения к сайту, его backend или production database.

## Безопасная схема

```text
Railway project "Taverna Shlyapnika"
├── main-site                 # существующий публичный сайт, не менять
├── main-site-postgres        # существующая production DB сайта, не использовать
├── control-frontend          # новый isolated service: UI + API, root apps/control-service
└── Postgres-jr3Q             # новый PostgreSQL только для Taverna Control
```

Запрещено на этом этапе:

- подключать `control-backend` к `DATABASE_URL` основного сайта;
- добавлять ссылки на кабинет в публичный сайт;
- включать `CONTROL_MAIN_SITE_INTEGRATION_ENABLED`;
- включать `CONTROL_TELEGRAM_INTEGRATION_ENABLED`;
- передавать путь запуска программ из браузера.

## Railway services

Текущий безопасный deploy сделан в том же Railway project, но отдельными ресурсами:

| Service | Root directory | Railway config | Назначение |
| --- | --- | --- | --- |
| `control-frontend` | `apps/control-service` | `railway.json` | Combined service: Spring Boot API + static admin UI |
| `Postgres-jr3Q` | Railway PostgreSQL | Railway managed | Isolated DB только для Control |

Название `control-frontend` осталось от первой попытки с двумя сервисами. Фактически это combined service, потому что Free plan Railway не дал создать отдельный backend service. Изоляция при этом сохраняется: root, Dockerfile, variables, cookie, schema и database отдельные, а публичный сайт не меняется.

Текущий deploy выполнен через Railway CLI из каталога `apps/control-service`, поэтому в service settings `rootDirectory` очищен. Если позже подключать GitHub autodeploy из корня репозитория, выставьте `rootDirectory=apps/control-service`.

Если позже тариф позволит больше ресурсов, можно разделить combined service на два:

| Service | Root directory | Railway config | Назначение |
| --- | --- | --- | --- |
| `control-backend` | `apps/control-service/backend` | `railway.json` | Spring Boot API, Flyway, isolated DB |
| `control-frontend` | `apps/control-service/frontend` | `railway.json` | Nginx static admin UI |

## Variables: combined control service

Минимальный набор:

```env
PORT=4190
CONTROL_DATABASE_URL=jdbc:postgresql://${{Postgres-jr3Q.PGHOST}}:${{Postgres-jr3Q.PGPORT}}/${{Postgres-jr3Q.PGDATABASE}}
CONTROL_DATABASE_USERNAME=${{Postgres-jr3Q.PGUSER}}
CONTROL_DATABASE_PASSWORD=${{Postgres-jr3Q.PGPASSWORD}}
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
CONTROL_STATIC_LOCATIONS=file:/app/static/
CONTROL_MEDIA_STORAGE_ROOT=/app/data/media
CONTROL_BACKUP_STORAGE_ROOT=/app/data/backups
```

`CONTROL_BOOTSTRAP_TOKEN` используется как первичный пароль только до создания первого OWNER. После первого входа смените пароль и уберите bootstrap variables из Railway.

## Frontend runtime API URL

```env
CONTROL_API_BASE=
```

Для combined service оставьте `CONTROL_API_BASE` пустым или не задавайте его: frontend будет обращаться к API same-origin. При разделении на два сервиса задайте `CONTROL_API_BASE=https://<control-backend>.up.railway.app`.

## Проверка после deploy

1. Откройте `https://<control-frontend>.up.railway.app/health`.
2. Откройте `https://<control-frontend>.up.railway.app/ready`.
3. Откройте `https://<control-frontend>.up.railway.app/`, выберите раздел `Безопасность`.
4. Войдите bootstrap OWNER.
5. Создайте отдельного пользователя через invitation.
6. Смените пароль OWNER и удалите `CONTROL_BOOTSTRAP_OWNER_EMAIL` / `CONTROL_BOOTSTRAP_TOKEN` из Railway variables.

## Что остаётся изолированным

- DB schema: `control_*` и `SPRING_SESSION*` в отдельной control database.
- API namespace: `/api/v1/**`, не совпадает с публичным сайтом.
- CORS: только `CONTROL_FRONTEND_ORIGIN`.
- Cookies: `CONTROLSESSION`, не используется сайтом.
- Desktop launch: только allowlist env, по умолчанию выключен.
