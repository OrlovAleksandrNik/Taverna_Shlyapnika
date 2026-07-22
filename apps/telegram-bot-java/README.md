# Taverna Shlyapnika Java Telegram Bot

Отдельный Java 21 + Spring Boot сервис Telegram-бота для постепенной замены текущего Node/grammY бота.

## Статус

Добавлено:

- Spring Boot приложение;
- health/readiness endpoints;
- polling loop через Telegram Bot API;
- удаление webhook перед polling;
- базовая команда `/start`;
- обработка одного update без остановки всего процесса;
- graceful shutdown;
- отсутствие прямого доступа к PostgreSQL.

Пока не перенесено:

- регистрация мастеров;
- меню мастера;
- создание и редактирование игр;
- галерея;
- рейтинг;
- persistent `BotSession`;
- idempotency callback-операций.

Node-бот нельзя выключать до полного переноса сценариев и staging-проверки.

## Локальный запуск без установленного Java и Maven

Из корня проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-java.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 bot --% test
powershell -ExecutionPolicy Bypass -File .\scripts\run-java-module.ps1 bot --% spring-boot:run -Dspring-boot.run.profiles=local
```

Через `pnpm`:

```bash
pnpm test:bot:java
pnpm dev:bot:java
pnpm build:bot:java
```

Maven-зависимости складываются в проектный кэш `.tools/m2`, глобальный Maven не нужен.

## Переменные

```text
TELEGRAM_BOT_TOKEN=
BOT_MODE=polling
JAVA_BACKEND_INTERNAL_URL=https://<java-backend-service>
BOT_BACKEND_SECRET=
PORT=8081
BOT_POLLING_TIMEOUT_SECONDS=25
```

Секреты нельзя хранить в Git и нельзя выводить в логи.

## Railway

Рекомендуется отдельный Railway service:

- root directory: `apps/telegram-bot-java`;
- Dockerfile: `apps/telegram-bot-java/Dockerfile`;
- replica count для polling: `1`;
- переменные: `TELEGRAM_BOT_TOKEN`, `JAVA_BACKEND_INTERNAL_URL`, `BOT_BACKEND_SECRET`, `BOT_MODE=polling`.

Polling выбран как стартовый режим, потому что текущая инфраструктура уже использует polling и так проще избежать настройки публичного webhook path. Перед запуском Java-бот удаляет старый webhook.

Нельзя одновременно запускать Node polling bot и Java polling bot на одном Telegram token.

## Health

```text
GET /health
GET /ready
GET /actuator/health/liveness
GET /actuator/health/readiness
```

`/health` показывает:

- включён ли bot token;
- запущен ли polling;
- режим;
- последний update ID;
- время последнего update.

## Архитектура

Бот не обращается напрямую к PostgreSQL.

Планируемый поток:

```text
Telegram
  -> Java Telegram Bot
  -> Java Backend Internal API
  -> Application Services
  -> PostgreSQL
```

Будущие мутации должны передавать:

- Telegram user ID;
- operation ID;
- idempotency key;
- callback query ID, если есть;
- internal token.
