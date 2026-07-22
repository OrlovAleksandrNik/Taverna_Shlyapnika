# Переменные окружения

Безопасный шаблон находится в `.env.example`.

## Обязательные

- `DATABASE_URL` — PostgreSQL connection string.
- `INTERNAL_API_TOKEN` — секрет для `/api/internal/*`, минимум 12 символов.

## Telegram

- `TELEGRAM_BOT_TOKEN` — токен от BotFather. Если пустой, сайт и API работают без бота.
- `BOT_MODE` — `polling` или `webhook`.
- `WEBHOOK_URL` — HTTPS URL для webhook-режима.
- `ADMIN_TELEGRAM_IDS` — список Telegram ID администраторов через запятую.
- `AUTO_PUBLISH` — если `true`, созданная мастером игра сразу получает статус `published`.

## Сайт и API

- `PORT` — HTTP-порт, по умолчанию `4177`.
- `SITE_BASE_URL` — базовый URL сайта и API.
- `SITE_ORIGINS` — разрешённые origins для CORS через запятую.
- `TAVERN_TIMEZONE` — часовой пояс отображения дат.

## Загрузки

- `FILE_STORAGE_DIR` — локальный каталог загрузок.
- `PUBLIC_UPLOADS_URL` — публичный URL для изображений.

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
