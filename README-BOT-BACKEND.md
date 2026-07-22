# Telegram-бот и backend для “Таверны Шляпника”

## Архитектура

Поток данных:

`Telegram-бот -> Backend/API -> PostgreSQL -> сайт, раздел “Афиша”`

Бот не редактирует HTML-файлы. Он создаёт и меняет записи в базе. Сайт запрашивает актуальные опубликованные будущие игры через `GET /api/games`.

## Что добавлено

- `apps/backend-java` — основной Spring Boot backend, API, статика сайта, CORS, заявки, афиша, галерея и рейтинг.
- `apps/telegram-bot-java` — основной Telegram-бот мастеров: регистрация, профиль, создание игры, галерея, рейтинг, просмотр, редактирование, отмена.
- `src/api/server.ts` и `src/bot/index.ts` — legacy Node-реализация, оставлена в проекте как резерв до полного удаления старого слоя.
- `src/services/games.ts` — публичные DTO, публикация статусов, автоархивация прошедших игр.
- `prisma/schema.prisma` — модели `Master`, `Game`, `AuditLog`, `BotSession`.
- `prisma/migrations/.../migration.sql` — начальная миграция PostgreSQL.
- `script.js` — раздел “Афиша” теперь читает игры через `/api/games`, показывает загрузку/ошибку/пустое состояние и обновляется каждые 45 секунд.
- `.env.example`, `Dockerfile`, `docker-compose.yml`, `package.json`, `tsconfig.json`.

## Локальный запуск

На этой машине уже подготовлен portable PostgreSQL в `.postgres` и каталог данных `.postgres-data`.

Запустить локальную базу, сайт/API и Telegram-бота:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Остановить приложение и локальную PostgreSQL:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

Сайт и API будут доступны на `http://localhost:4177`.

## Ручной локальный запуск

1. Установить зависимости:

```bash
npm install
```

2. Скопировать env:

```bash
cp .env.example .env
```

3. Заполнить в `.env`:

```env
TELEGRAM_BOT_TOKEN=токен_от_BotFather
DATABASE_URL=postgresql://taverna:taverna@localhost:5432/taverna_shlyapnika?schema=public
INTERNAL_API_TOKEN=длинная_случайная_строка
ADMIN_TELEGRAM_IDS=123456789,987654321
ADMIN_TELEGRAM_USERNAMES=MisterHatter
BOT_DISPLAY_NAME=Писарь Таверны
BOT_SHORT_DESCRIPTION=Создаёт афиши игр, ведёт галерею и помогает мастерам управлять рейтингом игроков.
BOT_CACHE_CHAT_ID=
BOT_CLEANUP_DELAY_SECONDS=86400
AUTO_PUBLISH=true
TAVERN_TIMEZONE=Europe/Minsk
```

4. Запустить PostgreSQL:

```bash
docker compose up -d postgres
```

5. Применить миграции:

```bash
npm run prisma:generate
npm run prisma:migrate
```

6. Запустить сайт, API и бота:

```bash
npm run dev
```

Сайт откроется на `http://localhost:4177`.

## Production

Для production используйте:

```bash
npm run build
npm run prisma:deploy
npm start
```

Локальная разработка работает через long polling. Для production можно включить `BOT_MODE=webhook` и задать `WEBHOOK_URL`, если платформа предоставляет публичный HTTPS URL.

## API сайта

`GET /api/games`

Возвращает будущие опубликованные игры. Параметры:

- `dateFrom`
- `dateTo`
- `masterId`
- `system`
- `limit`
- `offset`

`GET /api/games/:id`

Возвращает одну опубликованную будущую игру.

Внутренние операции находятся под `/api/internal/*` и требуют заголовок:

```http
x-internal-token: значение_INTERNAL_API_TOKEN
```

## Автоархивация

Прошедшие опубликованные игры переводятся в `completed`:

- автоматически каждые 10 минут при запущенном приложении;
- вручную командой `npm run archive`;
- через защищённый endpoint `POST /api/internal/archive-past-games`.
