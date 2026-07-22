# Развертывание

## Локальная разработка

Для обычной разработки сайта и старого Node-кода:

```bash
pnpm install
cp .env.example .env
docker compose up -d postgres
pnpm prisma:generate
pnpm prisma:deploy
pnpm dev
```

Для Java backend без установленного Java/Maven используйте переносной runner:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% spring-boot:run -Dspring-boot.run.profiles=local
```

## Production build Node

Node backend сохранен только как rollback-вариант:

```bash
pnpm install --frozen-lockfile
pnpm prisma:generate
pnpm build
pnpm prisma:deploy
pnpm start
```

Для Docker rollback используйте `Dockerfile.node`.

## Docker Compose

```bash
docker compose up -d postgres
docker compose up --build app
```

В production PostgreSQL и uploads должны жить в постоянных volumes или управляемых сервисах.

## Railway: основной сайт и Java backend

Корневой `Dockerfile` теперь собирает Java backend и копирует статический сайт в образ. Railway должен использовать:

- Builder: `DOCKERFILE`
- Dockerfile path: `Dockerfile`
- Start command: `java -jar /app/app.jar`
- Healthcheck path: `/health`

Java backend отдает:

- статический сайт из `/app/static-site`;
- публичные API `/api/*`;
- загрузки из `/uploads`;
- health endpoints `/health` и `/ready`.

Обязательные переменные Railway для web-сервиса:

- `DATABASE_URL` - строка подключения PostgreSQL из Railway Postgres.
- `BOT_BACKEND_SECRET` или `INTERNAL_API_TOKEN` - общий секрет для внутренних маршрутов backend и бота.
- `SITE_BASE_URL` - публичный URL сайта.
- `CORS_ALLOWED_ORIGINS` - публичный URL сайта без завершающего слеша.
- `TAVERN_TIMEZONE` - например `Europe/Minsk`.

Рекомендуемые переменные:

- `SERVE_FRONTEND=true`
- `FRONTEND_STATIC_DIR=/app/static-site`
- `FILE_STORAGE_DIR=/app/uploads`
- `PUBLIC_UPLOADS_URL=/uploads`
- `AUTO_PUBLISH=true`

Если `DATABASE_URL` не задан, production Java backend завершит запуск с понятной ошибкой. Это ожидаемо: афиша, заявки, рейтинг и Telegram-бот должны работать с постоянной общей базой.

Если в логах Railway виден `ZodError` по `DATABASE_URL`, сервис запускает старый Node deployment. Проверьте, что Railway деплоит ветку `main` с актуальным коммитом, builder выбран как Dockerfile, затем выполните Redeploy или Clear build cache and redeploy.

## Railway: Java Telegram-бот

Бот лучше запускать отдельным Railway service из этого же репозитория:

- Root directory: `apps/telegram-bot-java`
- Dockerfile path: `apps/telegram-bot-java/Dockerfile` или `Dockerfile`, если root directory уже выбран
- Replica count: `1`

Переменные бота:

- `TELEGRAM_BOT_TOKEN` - токен от BotFather.
- `JAVA_BACKEND_INTERNAL_URL` - внутренний URL Java backend web-сервиса Railway.
- `BOT_BACKEND_SECRET` или `INTERNAL_API_TOKEN` - тот же секрет, что у backend.
- `BOT_MODE=polling`

Для polling не включайте несколько реплик одного и того же бота, иначе Telegram может вернуть `409 Conflict`.

## HTTPS и Telegram

Для polling нужен только исходящий доступ к Telegram API.

Для webhook нужен публичный HTTPS URL:

```env
BOT_MODE=webhook
WEBHOOK_URL=https://example.com/<telegram-webhook-path>
```

Перед включением webhook убедитесь, что hosting действительно принимает запросы Telegram.

## Файлы загрузок

Файлы хранятся в `FILE_STORAGE_DIR` и отдаются через `PUBLIC_UPLOADS_URL`. Каталог uploads не должен попадать в Git. На Railway для долгого хранения нужен Volume или внешний storage.

## Миграции

Java backend применяет Flyway-миграции при старте приложения. Node/Prisma-миграции больше не должны запускаться в основном Railway web-сервисе после переключения на Java.

## Health-check

```http
GET /health
```

Возвращает состояние backend, базы и режима бота без вывода секретов. Этот маршрут используется Railway как liveness healthcheck.

```http
GET /ready
```

Возвращает `503`, если база недоступна. Используйте его для внутренней диагностики, а не как Railway liveness healthcheck.
