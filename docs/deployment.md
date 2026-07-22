# Развёртывание

## Локальная разработка

```bash
pnpm install
cp .env.example .env
docker compose up -d postgres
pnpm prisma:generate
pnpm prisma:deploy
pnpm dev
```

## Production build

```bash
pnpm install --frozen-lockfile
pnpm prisma:generate
pnpm build
pnpm prisma:deploy
pnpm start
```

## Docker Compose

```bash
docker compose up -d postgres
docker compose up --build app
```

В production вынесите PostgreSQL и uploads в постоянные volumes или управляемые сервисы.

## Railway

В репозитории есть `railway.json`, который заставляет Railway использовать `Dockerfile`.
Также добавлен `nixpacks.toml` с тем же build/start-сценарием на случай, если сервис в Dashboard настроен на Railpack/Nixpacks.

Перед запуском контейнер выполняет:

```bash
pnpm prisma:deploy # только если DATABASE_URL задан
node dist/index.js
```

Обязательные переменные Railway:

- `DATABASE_URL` - строка подключения PostgreSQL.
- `INTERNAL_API_TOKEN` - длинная случайная строка для внутренних маршрутов.
- `TELEGRAM_BOT_TOKEN` - нужен только если бот должен работать на Railway.
- `BOT_MODE=polling` - для простого запуска одной реплики.
- `SITE_BASE_URL` - публичный Railway URL.
- `SITE_ORIGINS` - публичный Railway URL без завершающего слеша.
- `PUBLIC_UPLOADS_URL` - `https://<ваш-домен>/uploads`.

Healthcheck Railway указывает на `/health`. Этот маршрут отвечает `200`, если HTTP-процесс жив, и отдельно показывает состояние базы. Если `DATABASE_URL` не задан, `/health` вернёт `database: "not_configured"`, а `/ready` вернёт `503`.

Важно: без `DATABASE_URL` статический сайт поднимется, но афиша, заявки, рейтинг и Telegram-бот не смогут работать с реальными данными.

Если в логах всё ещё видно `ZodError` по обязательному `DATABASE_URL`, Railway запустил старый deployment. Проверьте, что сервис деплоит ветку `main` и commit не ниже `2a8748d`, затем выполните Redeploy или Clear build cache and redeploy.

## HTTPS и Telegram

Для polling нужен только исходящий доступ к Telegram API.

Для webhook нужен публичный HTTPS URL:

```env
BOT_MODE=webhook
WEBHOOK_URL=https://example.com/<telegram-webhook-path>
```

Перед включением webhook нужно убедиться, что hosting действительно принимает запросы Telegram.

## Файлы загрузок

Загруженные изображения игр хранятся в `FILE_STORAGE_DIR` и отдаются через `PUBLIC_UPLOADS_URL`. Этот каталог не должен попадать в Git.

## Миграции

В production используйте:

```bash
pnpm prisma:deploy
```

Не используйте `prisma migrate dev` на production-базе.

## Резервное копирование

Минимально сохраняйте:

- PostgreSQL dump;
- каталог uploads;
- `.env` в защищённом хранилище секретов.

Не храните резервные копии в публичном репозитории.

## Health-check

```http
GET /health
```

Ответ показывает состояние backend, базы, режима бота и время последнего Telegram update без вывода секретов.

```http
GET /ready
```

Возвращает `503`, если база недоступна. Этот endpoint лучше использовать для внутренней диагностики, а не как Railway liveness healthcheck.
