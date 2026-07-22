# Data migration plan

## Текущее состояние

Проект уже использует PostgreSQL и Prisma migrations. Java backend подключается к этой же базе через `DATABASE_URL`.

## Безопасная схема перехода

1. Поднять Java backend как отдельный Railway service.
2. Подключить тот же PostgreSQL service.
3. Проверить `/health` и `/ready`.
4. Проверить публичные endpoints:
   - `/api/games`
   - `/api/gallery`
   - `/api/rating`
5. Проверить создание заявки:
   - `/api/game-signups`
   - `/api/service-requests`
6. Переключить frontend API URL на Java service.
7. Переключить Telegram-бота на Java internal endpoints либо перенести bot process в Java.
8. После стабильной проверки выключить старый Node backend.

## Flyway

Для пустой базы Java применяет:

```text
apps/backend-java/src/main/resources/db/migration/V1__baseline_existing_prisma_schema.sql
```

Для существующей Prisma-базы включено:

```text
spring.flyway.baseline-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
```

Это предотвращает пересоздание таблиц и удаление пользовательских данных.

## Rollback

Пока Node backend не удалён, откат выполняется переключением API URL обратно на старый сервис.

Перед окончательным удалением старого backend нужно подтвердить:

- Java API отдаёт реальные игры из базы;
- заявки сохраняются с фактом согласия;
- Telegram публикация игры создаёт `published` запись;
- прошедшие игры архивируются;
- gallery/rating endpoints совпадают по контракту с frontend.

## Переключение Telegram-бота

Безопасный порядок:

1. Поднять `apps/telegram-bot-java` с тестовым token или в отдельном staging окружении.
2. Проверить `/health` и `/ready`.
3. Проверить `/start`.
4. Перенести сценарии игр, рейтинга и галереи на Java internal API.
5. Проверить, что состояния хранятся в PostgreSQL, а не в памяти процесса.
6. Остановить Node polling bot.
7. Убедиться, что нет второго polling process.
8. Запустить Java polling bot с production token.
9. Проверить создание игры, публикацию галереи, рейтинг и уведомления.
10. При сбое остановить Java bot и вернуть Node bot.

Один Telegram token не должен одновременно использоваться двумя polling-сервисами.

## Railway services

Минимальная схема staging:

- `frontend` или текущий Node service как временный статический frontend/API fallback;
- `backend-java` из `apps/backend-java`;
- `telegram-bot-java` из `apps/telegram-bot-java`;
- общий PostgreSQL service;
- Railway Volume или S3-compatible storage для uploads.

Обязательные переменные Java backend:

```text
DATABASE_URL
BOT_BACKEND_SECRET
SITE_BASE_URL
CORS_ALLOWED_ORIGINS
PUBLIC_UPLOADS_URL
FILE_STORAGE_DIR
TAVERN_TIMEZONE
MEDIA_STORAGE
```

Обязательные переменные Java bot:

```text
TELEGRAM_BOT_TOKEN
BOT_MODE=polling
JAVA_BACKEND_INTERNAL_URL
BOT_BACKEND_SECRET
```
