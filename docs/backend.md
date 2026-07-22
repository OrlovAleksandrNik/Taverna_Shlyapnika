# Backend API

Backend находится в `src/api/server.ts` и запускает Express.

## Публичные маршруты

- `GET /health` - состояние backend, базы и Telegram-бота.
- `GET /api/games` - будущие опубликованные игры.
- `GET /api/games/:id` - одна будущая опубликованная игра.
- `GET /api/rating` - публичный рейтинг.
- `POST /api/game-signups` - запись игрока на конкретную игру.
- `POST /api/service-requests` - заявка на услугу.

## Внутренние маршруты

Все `/api/internal/*` требуют заголовок:

```http
x-internal-token: <INTERNAL_API_TOKEN>
```

Маршруты:

- `POST /api/internal/games` - создать игру из внутреннего источника.
- `PATCH /api/internal/games/:id/status` - изменить статус игры.
- `POST /api/internal/archive-past-games` - архивировать прошедшие игры.
- `POST /api/internal/privacy/withdraw-consent` - отметить отзыв согласия и при необходимости обезличить заявку.

## Ошибки

Если согласие на обработку персональных данных не передано или версии не совпали, backend возвращает `CONSENT_REQUIRED`.

В ответах пользователю не раскрываются Prisma-ошибки, токены и технические секреты.

## CORS

Origins задаются через `SITE_ORIGINS`. Для локального `file://` разрешается origin `null`, но полноценная интеграция должна проверяться через HTTP.
