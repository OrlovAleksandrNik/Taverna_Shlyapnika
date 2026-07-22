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
