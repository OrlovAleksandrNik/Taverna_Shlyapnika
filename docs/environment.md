# Переменные окружения

Безопасный шаблон находится в `.env.example`.

## Обязательные

- `DATABASE_URL` - PostgreSQL connection string.
- `BOT_BACKEND_SECRET` или `INTERNAL_API_TOKEN` - секрет для `/api/internal/*`, минимум 12 символов.

На Railway `DATABASE_URL` добавляется из PostgreSQL-сервиса в Variables web-сервиса. В production Java backend не стартует без этой переменной, потому что реальные афиша, заявки, рейтинг и Telegram-бот должны использовать постоянную общую базу.

## Telegram

- `TELEGRAM_BOT_TOKEN` - токен от BotFather. Не храните его в Git и не выводите в логи.
- `BOT_MODE` - `polling` или `webhook`.
- `WEBHOOK_URL` - HTTPS URL для webhook-режима.
- `ADMIN_TELEGRAM_IDS` - список Telegram ID администраторов через запятую.
- `AUTO_PUBLISH` - если `true`, созданная мастером игра сразу получает статус `published`.
- `JAVA_BACKEND_INTERNAL_URL` - URL Java backend для отдельного Java Telegram-бота.

## Сайт и API

- `PORT` - HTTP-порт, в Railway задается платформой.
- `SITE_BASE_URL` - базовый публичный URL сайта и API.
- `SITE_ORIGINS` - совместимое старое имя списка CORS origins.
- `CORS_ALLOWED_ORIGINS` - разрешенные origins для CORS через запятую.
- `TAVERN_TIMEZONE` - часовой пояс отображения дат, например `Europe/Minsk`.
- `SERVE_FRONTEND` - если `true`, Java backend отдает статический сайт.
- `FRONTEND_STATIC_DIR` - каталог HTML/CSS/JS для отдачи через Java backend.

## Загрузки

- `FILE_STORAGE_DIR` - локальный каталог загрузок.
- `PUBLIC_UPLOADS_URL` - публичный URL для файлов.
- `MEDIA_STORAGE` - тип хранилища, сейчас основной вариант `local`.

На Railway для `FILE_STORAGE_DIR=/app/uploads` нужен Volume, если файлы должны переживать redeploy.

## Юридические и контактные placeholders

Эти значения сейчас также есть в `data/site-settings.js` для frontend:

- `TELEGRAM_COMMUNITY_URL`
- `PHONE_NUMBER`
- `UNP`
- `OPERATOR_NAME`
- `OPERATOR_ADDRESS`
- `PRIVACY_CONTACT`
- `CONSENT_WITHDRAWAL_CONTACT`

Не придумывайте официальные реквизиты. Заполните их после утверждения владельцем.
