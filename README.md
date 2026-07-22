# Таверна Шляпника

Сайт, backend и Telegram-бот для клуба настольных ролевых игр «Таверна Шляпника» в Могилёве.

Проект объединяет публичный сайт, афишу игр, заявки игроков, страницу услуг, профили мастеров, дневник Шляпника, публичный рейтинг игроков и Telegram-бота для мастеров и администраторов.

## Возможности

- Статический сайт с главной страницей, услугами, галереей, дневником, рейтингом и страницами мастеров.
- Афиша игр строится через `GET /api/games` из PostgreSQL, без mock-карточек.
- Telegram-бот на grammY позволяет мастерам регистрироваться, создавать игры, отменять их и смотреть свои записи.
- Администраторы через бота управляют рейтингом игроков: добавляют участников, сыгранные игры, очки и вдохновение.
- Backend на Express сохраняет заявки на игры и услуги, проверяет согласие на обработку персональных данных и отдаёт публичное API.
- Prisma хранит схему, миграции и связи между мастерами, играми, заявками, согласием, аудитом и рейтингом.

## Технологии

- Node.js 22
- TypeScript
- Express
- grammY
- Prisma
- PostgreSQL
- pnpm
- Docker Compose для локальной базы и контейнерного запуска

## Быстрый запуск

```bash
pnpm install
cp .env.example .env
pnpm prisma:generate
pnpm prisma:deploy
pnpm dev
```

Сайт и API по умолчанию доступны на `http://localhost:4177`.

На Windows для локальной разработки можно использовать готовый сценарий:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Остановка:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

## Переменные окружения

Создайте `.env` из `.env.example` и заполните реальные значения. Не коммитьте `.env`.

Ключевые переменные:

- `DATABASE_URL` — строка подключения PostgreSQL.
- `TELEGRAM_BOT_TOKEN` — токен от BotFather.
- `BOT_MODE` — `polling` для локального запуска или `webhook` для production.
- `WEBHOOK_URL` — публичный HTTPS webhook, если используется webhook-режим.
- `INTERNAL_API_TOKEN` — секрет для внутренних API-операций.
- `ADMIN_TELEGRAM_IDS` — Telegram ID администраторов через запятую.
- `AUTO_PUBLISH` — публиковать ли созданные ботом игры сразу.
- `SITE_BASE_URL` и `SITE_ORIGINS` — публичный адрес и CORS origins.
- `TAVERN_TIMEZONE` — часовой пояс проекта, сейчас `Europe/Minsk`.
- `PUBLIC_UPLOADS_URL` и `FILE_STORAGE_DIR` — публичный URL и каталог загрузок.

Контакты и юридические placeholders редактируются в `data/site-settings.js` и на странице `privacy-policy.html`.

## Команды

```bash
pnpm dev              # сайт, API и бот одним процессом
pnpm dev:backend      # только Express API и статика
pnpm dev:bot          # только Telegram-бот
pnpm build            # сборка TypeScript в dist
pnpm start            # production-запуск dist/index.js
pnpm prisma:generate  # генерация Prisma Client
pnpm prisma:migrate   # локальная dev-миграция
pnpm prisma:deploy    # применение миграций в production
pnpm archive          # ручная архивация прошедших игр
pnpm check            # typecheck и prisma validate
pnpm test             # сейчас алиас на pnpm check
```

## Структура

```text
.
├── assets/              # публичные изображения, шрифты и исходники дневника
├── data/                # настройки сайта и статические данные
├── docs/                # документация проекта
├── masters/             # страницы мастеров
├── prisma/              # схема и миграции PostgreSQL
├── scripts/             # локальный запуск и сервисные сценарии
├── src/
│   ├── api/             # Express API, CORS, rate limit
│   ├── bot/             # Telegram-бот
│   ├── jobs/            # фоновые задачи
│   ├── services/        # бизнес-логика игр, заявок, рейтинга, privacy
│   └── utils/           # время и валидация
├── *.html               # страницы публичного сайта
├── script.js            # frontend-логика статического сайта
└── styles.css           # общие стили сайта
```

Проект оставлен в текущей компактной структуре, потому что сайт работает как набор статических страниц из корня, а backend отдаёт эти файлы напрямую. Искусственный перенос в `apps/*` потребует отдельной миграции путей.

## База данных

PostgreSQL создаётся миграциями Prisma. Основные модели: `Master`, `Game`, `GameSignup`, `ServiceRequest`, `ContactRequest`, `RatingPlayer`, `RatingPlayedGame`, `RatingEvent`, `AuditLog`, `BotSession`.

Подробности: `docs/database.md`.

Backend API подробно описан в `docs/backend.md`.

## Telegram-бот

Бот запускается вместе с основным процессом, если заполнен `TELEGRAM_BOT_TOKEN`, или отдельно командой `pnpm dev:bot`.

Локально используется long polling. Перед запуском polling бот удаляет старый webhook. Production может использовать webhook при заполненных `BOT_MODE=webhook` и `WEBHOOK_URL`.

Подробности: `docs/telegram-bot.md`.

## Рейтинг

Публичная страница `rating.html` читает `GET /api/rating`. Изменения рейтинга выполняются через Telegram-бота администратора и сохраняются как события, чтобы историю можно было проверить и восстановить.

Подробности: `docs/rating-system.md`.

## Docker

```bash
docker compose up -d postgres
docker compose up --build app
```

Файлы загрузок и база должны храниться в volumes, а не в Git.

## Безопасность

- `.env`, токены, база, логи, загруженные пользователями файлы и резервные копии исключены из Git.
- При утечке Telegram-токена его нужно немедленно отозвать у BotFather и выпустить новый.
- Не добавляйте реальные заявки, телефоны, приватные Telegram ID и production-дампы в репозиторий.
- Лицензия проекта пока не определена владельцем.

Подробности: `SECURITY.md`.

## Текущий статус

Проект готовится как единый Node/TypeScript-репозиторий со статическим frontend. Автоматические тесты пока не выделены отдельно; команда `pnpm test` выполняет проверку типов и Prisma-схемы.
