# Taverna Shlyapnika Java Backend

Java 21 + Spring Boot 3 backend для постепенной замены текущего Node.js/Express backend.

## Статус миграции

Перенесены:

- `GET /health`
- `GET /ready`
- `GET /api/games`
- `GET /api/games/{id}`
- `POST /api/game-signups`
- `POST /api/service-requests`
- `GET /api/gallery/categories`
- `GET /api/gallery`
- `GET /api/gallery/{publicId}`
- `GET /api/rating`
- `GET /api/internal/masters/by-telegram/{telegramUserId}`
- `POST /api/internal/masters`
- `GET /api/internal/masters/{masterId}/games`
- `PATCH /api/internal/masters/{masterId}/games/{gameId}`
- `PATCH /api/internal/masters/{masterId}/games/{gameId}/status`
- `GET /api/internal/masters/{masterId}/gallery-posts`
- `POST /api/internal/masters/{masterId}/gallery-posts`
- `PATCH /api/internal/masters/{masterId}/gallery-posts/{postId}/status`
- `GET /api/internal/bot-sessions/{telegramUserId}`
- `POST /api/internal/bot-sessions/{telegramUserId}`
- `DELETE /api/internal/bot-sessions/{telegramUserId}`
- `POST /api/internal/games`
- `PATCH /api/internal/games/{id}/status`
- `POST /api/internal/archive-past-games`
- `POST /api/internal/privacy/withdraw-consent`

Пока не перенесено полностью:

- все сценарии Telegram-бота;
- загрузка и обработка медиа из Telegram;
- административные сценарии рейтинга через Telegram.

Node backend намеренно не удалён: он остаётся рабочим ориентиром до проверки Java-сервиса на production-базе.

## Локальный запуск без установленного Java и Maven

В репозитории есть bootstrap для переносного JDK 21 и Maven Wrapper. Глобальный Maven не нужен.

Из корня проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-java.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% test
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% verify
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% spring-boot:run -Dspring-boot.run.profiles=local
```

Если используете `pnpm`, Java-команды из `package.json` вызывают тот же PowerShell-runner:

```bash
pnpm test:backend:java
pnpm verify:backend:java
pnpm dev:backend:java
```

Maven-зависимости складываются в проектный кэш `.tmp/m2`, поэтому текущая среда не зависит от пользовательского `~/.m2`.

## Docker

Docker не обязателен для локальных unit-тестов и обычного `verify`.

Интеграционные тесты с PostgreSQL Testcontainers включаются отдельно:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% verify -Dskip.unit.tests=true -Dskip.integration.tests=false
```

В CI интеграционные тесты включаются явно, потому что на GitHub Actions Docker доступен штатно.

## Основные переменные

```text
DATABASE_URL=postgresql://user:password@host:5432/db
BOT_BACKEND_SECRET=...
SITE_BASE_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:4177,http://localhost:8080,http://localhost:3000
PUBLIC_UPLOADS_URL=http://localhost:8080/uploads
FILE_STORAGE_DIR=uploads
TAVERN_TIMEZONE=Europe/Minsk
AUTO_PUBLISH=true
```

`DATABASE_URL` совместим с Railway/Postgres URL. Для явного JDBC URL можно использовать `DATABASE_JDBC_URL`.

## Railway

Для отдельного Java-сервиса:

1. Создать Railway service из этого же репозитория.
2. Указать root directory: `apps/backend-java`.
3. Использовать Dockerfile из этой папки.
4. Подключить тот же PostgreSQL service, что использует текущий backend.
5. Добавить переменные окружения без секретов в коде: `DATABASE_URL`, `BOT_BACKEND_SECRET`, `SITE_BASE_URL`, `CORS_ALLOWED_ORIGINS`, `PUBLIC_UPLOADS_URL`, `FILE_STORAGE_DIR`, `TAVERN_TIMEZONE`.

Проверка после запуска:

```bash
curl https://<java-service>/health
curl https://<java-service>/api/games
curl -X POST https://<java-service>/api/internal/archive-past-games -H "x-internal-token: <BOT_BACKEND_SECRET>"
```

## Тесты и отчёты

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% test
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 backend --% verify
```

Отчёты:

```text
target/surefire-reports
target/failsafe-reports
target/site/jacoco
```

JaCoCo выключен локально по умолчанию, чтобы не ломаться на Windows-пути текущей среды. В CI он включается через `-Djacoco.skip=false`.

## Security

- `/api/internal/**` защищён `x-internal-token`.
- Публичные POST endpoints защищены простым in-memory rate limit.
- CORS берётся из `CORS_ALLOWED_ORIGINS`.
- Ошибки возвращают и `message`, и совместимое поле `error`.
- Логи запросов содержат request ID, method, path, status и duration.
- Нельзя логировать токены, `DATABASE_URL`, полный телефон и содержимое файлов.

## База данных

Flyway содержит `V1__baseline_existing_prisma_schema.sql`.

- Пустая база создаётся Java-сервисом.
- Существующая Prisma-база принимается через `baseline-on-migrate=true`, без пересоздания таблиц.
- `ddl-auto=validate`; Java не меняет schema автоматически в production.
