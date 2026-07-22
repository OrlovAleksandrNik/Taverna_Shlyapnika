# Аудит backend перед миграцией на Java

Дата аудита: 2026-07-22

## Текущий стек

Backend проекта сейчас написан на Node.js/TypeScript.

- Runtime: Node.js, ESM.
- HTTP framework: Express.
- Telegram bot: grammy + @grammyjs/runner.
- ORM и миграции: Prisma.
- База данных: PostgreSQL.
- Валидация: Zod.
- Логирование: Pino.
- Даты: Luxon.
- Изображения: sharp.
- Production: Railway, Dockerfile, volume `/app/uploads`.

Frontend остаётся статическим HTML/CSS/JS и обращается к backend через REST API.

## Production-запуск

Основная точка входа:

- `src/index.ts`
- `src/api/server.ts`
- `src/bot/index.ts`

Команды из `package.json`:

- `pnpm run dev` — общий dev-запуск.
- `pnpm run dev:backend` — API.
- `pnpm run dev:bot` — Telegram-бот.
- `pnpm run build` — Prisma generate + TypeScript compile.
- `pnpm run start` — запуск `dist/index.js`.
- `pnpm run prisma:deploy` — применение Prisma migrations.

Railway запускает Node backend через `railway.json` и Dockerfile. Старый backend нельзя удалять до полной проверки Java-версии.

## Переменные окружения

Текущий `src/config.ts` читает:

- `NODE_ENV`
- `PORT`
- `DATABASE_URL`
- `TELEGRAM_BOT_TOKEN`
- `BOT_MODE`
- `WEBHOOK_URL`
- `INTERNAL_API_TOKEN`
- `ADMIN_TELEGRAM_IDS`
- `AUTO_PUBLISH`
- `SITE_BASE_URL`
- `SITE_ORIGINS`
- `TAVERN_TIMEZONE`
- `PUBLIC_UPLOADS_URL`
- `FILE_STORAGE_DIR`

Риски:

- `DATABASE_URL` Prisma содержит все параметры подключения одной строкой. Spring Boot удобнее работает с `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, но Railway может отдавать именно JDBC-compatible или postgres URL. Нужно поддержать оба варианта.
- `TELEGRAM_BOT_TOKEN` нельзя логировать.
- `PUBLIC_UPLOADS_URL` должен совпадать с публичным URL Java backend.

## Публичные HTTP endpoints

Найдено в `src/api/server.ts`.

| Endpoint | Метод | Назначение | Текущий формат |
|---|---:|---|---|
| `/health` | GET | health backend, БД и бота | `{ ok, backend, database, bot, checkedAt, latencyMs }` |
| `/ready` | GET | readiness с кодом 200/503 по БД | `{ ok, backend, database, bot, checkedAt, latencyMs }` |
| `/api/games` | GET | публичная афиша | `{ games: [...] }` |
| `/api/games/:id` | GET | публичная карточка игры | `{ game }` или 404 |
| `/api/rating` | GET | публичный рейтинг | `{ topThree, players, total, sort, updatedAt }` |
| `/api/gallery/categories` | GET | категории галереи | `{ categories: [{ value }] }` |
| `/api/gallery` | GET | публичная галерея | `{ posts: [...] }` |
| `/api/gallery/:publicId` | GET | публичная публикация галереи | `{ post }` или 404 |
| `/api/game-signups` | POST | запись игрока на игру | `{ ok, message, signupId, game }` |
| `/api/service-requests` | POST | заявка на услугу | `{ ok, message, requestId }` |

## Внутренние HTTP endpoints

Все endpoints под `/api/internal` защищены заголовком `x-internal-token`.

| Endpoint | Метод | Назначение |
|---|---:|---|
| `/api/internal/archive-past-games` | POST | архивирует прошедшие опубликованные игры |
| `/api/internal/games` | POST | создаёт игру из внутреннего источника |
| `/api/internal/games/:id/status` | PATCH | меняет статус игры |
| `/api/internal/privacy/withdraw-consent` | POST | отзыв согласия и опциональное обезличивание |

Риск: бизнес-операции Telegram-бота сейчас часто обращаются к Prisma напрямую, минуя внутреннее API. В Java-версии проверка прав и бизнес-логика должны быть перенесены в сервисы Java.

## Таблицы и модели

Текущая схема описана в `prisma/schema.prisma`.

### Master

Хранит Telegram-профиль мастера и публичный контакт.

Поля:

- `id`
- `telegramUserId`
- `telegramUsername`
- `displayName`
- `contactUrl`
- `role`: `master | admin`
- `status`: `active | blocked`
- timestamps

Связи:

- `games`
- `gameSignups`
- `galleryPosts`

### Game

Афиша игр.

Поля:

- `id`
- `masterId`
- `title`
- `description`
- `gameSystem`
- `experienceLevel`
- `ageRating`
- `dateTimeStart`
- `durationMinutes`
- `dateTimeEnd`
- `minPlayers`
- `maxPlayers`
- `price` Decimal(10,2)
- `currency`
- `imageUrl`
- `contactUrl`
- `status`: `draft | pending | published | completed | cancelled | archived`
- timestamps and status timestamps

Публичная афиша показывает только будущие `published`.

### GameSignup

Запись игрока на игру.

Поля:

- `gameId`
- `masterId`
- `playerName`
- `contact`
- `seats`
- `comment`
- `status`: `pending | confirmed | cancelled`
- поля согласия: `consentGiven`, `consentVersion`, `privacyPolicyVersion`, `consentedAt`, `formType`, `consentWithdrawnAt`, `dataUseStoppedAt`, `anonymizedAt`

Уникальность: `gameId + contact`.

### ServiceRequest

Заявка на услугу.

Поля:

- `name`
- `contact`
- `service`
- `desiredDate`
- `participants`
- `city`
- `comment`
- `status`: `new | contacted | closed`
- поля согласия

### ContactRequest

Старая/подготовленная модель формы контактов. Сейчас форма контактов на frontend удалялась, но модель есть.

### GalleryPost

Публикации галереи.

Поля:

- `publicId`
- `type`: `photo | story | character_sheet`
- `title`
- `description`
- `storyContent`
- `storyHtml`
- `category`
- `eventDate`
- `authorMasterId`
- `status`: `draft | published | hidden`
- `isVisible`
- `sortOrder`
- `publishedAt`
- timestamps

Публичный API отдаёт только `published` и `isVisible=true`.

### GalleryMedia

Файлы публикации галереи.

Поля:

- `galleryPostId`
- `fileUrl`
- `thumbnailUrl`
- `mediumUrl`
- `width`
- `height`
- `mimeType`
- `altText`
- `sortOrder`

### RatingPlayer

Публичный игрок рейтинга.

Поля:

- `displayName`
- `nickname`
- `avatarUrl`
- `isVisible`
- агрегаты `gamesPlayed`, `totalPoints`, `inspirationCount`
- `lastGameAt`
- `lastStatsAt`

Место в рейтинге не хранится, считается запросом.

### RatingPlayedGame

Игра/событие для журнала рейтинга.

### RatingEvent

Журнал операций рейтинга.

Типы:

- `player_created`
- `player_updated`
- `player_hidden`
- `player_shown`
- `game_result`
- `points_adjustment`
- `inspiration_adjustment`
- `correction`

Есть `idempotencyKey` с unique index, но часть текущих операций бота не всегда передаёт ключ. В Java это нужно усилить.

### AuditLog

Служебный аудит действий.

### BotSession

Состояние Telegram-сценариев. Хранит `telegramUserId`, `state`, `draft`.

## Бизнес-логика афиши

Файл: `src/services/games.ts`.

- Перед публичной выдачей вызывается `archivePastGames()`.
- Архивирование: `published` игра становится `archived`, если `dateTimeEnd < now` или нет `dateTimeEnd`, но `dateTimeStart` старше 6 часов.
- Публичные игры: только `published`, `dateTimeStart >= now`, сортировка по ближайшей дате.
- `availableSeats = maxPlayers - confirmedSeats`.
- При записи проверяется согласие версии `1.0`.
- Запись выполняется upsert по `gameId + contact`.
- После записи отправляется уведомление мастеру и администраторам через Telegram API.

## Бизнес-логика заявок и персональных данных

Файлы:

- `src/services/consent.ts`
- `src/services/serviceRequests.ts`
- `src/services/privacy.ts`

Правило:

- `consentGiven=true`
- `consentVersion="1.0"`
- `privacyPolicyVersion="1.0"`

Иначе ошибка `CONSENT_REQUIRED`.

Отзыв согласия умеет отмечать прекращение использования данных и обезличивать:

- `GameSignup`
- `ServiceRequest`
- `ContactRequest`

## Бизнес-логика рейтинга

Файл: `src/services/rating.ts`.

- Рейтинг строится из `RatingPlayer` и `RatingEvent`.
- Сортировка: `totalPoints DESC`, `averagePointsPerGame DESC`, `gamesPlayed DESC`, `displayName ASC`, `id ASC`.
- Среднее считается SQL-выражением с `numeric`, округляется до 2 знаков.
- Изменения рейтинга выполняются транзакционно.
- После каждого события пересчитываются агрегаты игрока.
- Есть уникальный `idempotencyKey`, но не все вызовы его используют.

Риски:

- Логика частично завязана на raw SQL.
- Нужно явно перенести оконные функции ранжирования в Java/JPA native query или projection.
- Не использовать `double`; среднее считать через `BigDecimal`.

## Бизнес-логика галереи

Файл: `src/services/gallery.ts`.

- Поддерживаются категории: `games`, `events`, `heroes`, `tavern`, `miniatures`, `other`.
- Типы: `photo`, `story`, `character_sheet`.
- Статусы: `draft`, `published`, `hidden`.
- Telegram-файлы скачиваются через Telegram file API.
- Проверяется реальная сигнатура JPEG/PNG/WEBP.
- Ограничение файла: 12 МБ.
- Оригинал сохраняется в `FILE_STORAGE_DIR/gallery/<publicId>/`.
- `sharp` создаёт `medium.webp` и `thumb.webp`.
- Публичные URL строятся через `PUBLIC_UPLOADS_URL`.
- `storyHtml` формируется из ограниченной Markdown-разметки.

Риски:

- В Java нужен аналог обработки изображений: TwelveMonkeys/ImageIO, Thumbnailator, imgscalr или webp-imageio. WEBP-кодирование потребует проверки зависимости в Docker.
- Нужно сохранить существующие URL и структуру файлов, чтобы старые публикации не пропали.

## Telegram-бот

Файл: `src/bot/index.ts`.

Сценарии:

- регистрация мастера;
- создание игры для афиши;
- просмотр своих игр;
- отмена/редактирование игры;
- профиль мастера;
- галерея: добавление фото, истории, публикация/черновик/скрытие;
- рейтинг: создание игрока, начисление игры, корректировка очков, вдохновение, видимость игрока, история;
- административная статистика.

Роли сейчас:

- `master`
- `admin`
- плюс список `ADMIN_TELEGRAM_IDS`.

Требование Java-миграции расширяет роли до:

- `MASTER`
- `CONTENT_MANAGER`
- `RATING_MANAGER`
- `SUPERADMIN`

Риск: текущие роли нужно мигрировать без потери доступа. Минимальное правило совместимости: `admin` -> `SUPERADMIN`, `master` -> `MASTER`.

## Фоновые задачи

- `archivePastGames()` вызывается при публичных запросах и доступен внутренним endpoint.
- `src/jobs/archivePastGames.ts` запускает архивирование отдельно.

В Java лучше сделать:

- scheduled job через `@Scheduled`;
- внутренний endpoint для ручного запуска;
- transactional update.

## Ошибки и безопасность

Сейчас:

- Zod errors -> 422.
- Consent errors -> 422 с `CONSENT_REQUIRED`.
- Internal API protected by shared token.
- CORS проверяет `SITE_ORIGINS`, разрешает `null` для file://.
- Логи не должны содержать Telegram token.

Для Java:

- `@RestControllerAdvice`.
- единый JSON ошибки с `code`, `message`, `timestamp`, `path`.
- Spring Security для внутренних endpoints.
- Correlation ID filter.

## Совместимость API

Поля, критичные для frontend:

### `/api/games`

`games[]`:

- `id`
- `title`
- `description`
- `system`
- `gameSystem`
- `experienceLevel`
- `ageRating`
- `dateTimeStart`
- `dateTimeEnd`
- `startsAtLabel`
- `durationMinutes`
- `minPlayers`
- `maxPlayers`
- `bookedSeats`
- `availableSeats`
- `price`
- `currency`
- `contactUrl`
- `status`
- `masterId`
- `masterName`
- `masterTelegramUsername`
- `systemName`
- `master`
- `tags`

### `/api/rating`

- `topThree`
- `players`
- `total`
- `sort`
- `updatedAt`

`players[]`:

- `rank`
- `id`
- `displayName`
- `nickname`
- `avatarUrl`
- `gamesPlayed`
- `totalPoints`
- `inspirationCount`
- `averagePointsPerGame`
- `lastGameAt`
- `lastStatsAt`

### `/api/gallery`

`posts[]`:

- `id`
- `publicId`
- `type`
- `title`
- `description`
- `storyHtml`
- `category`
- `eventDate`
- `master`
- `media`
- `createdAt`
- `publishedAt`

`media[]`:

- `id`
- `fileUrl`
- `thumbnailUrl`
- `mediumUrl`
- `width`
- `height`
- `mimeType`
- `altText`

## Слабые места текущей реализации

- Telegram handlers напрямую обращаются к Prisma и содержат часть бизнес-логики.
- Роли слишком грубые: `master/admin`.
- Идемпотентность рейтинга частичная.
- Нет контрактных тестов API.
- Нет Java-compatible миграционного слоя.
- WEBP pipeline зависит от `sharp`; в Java нужен новый проверенный pipeline.
- `DATABASE_URL` формат нужно адаптировать для Spring/Railway.
- Старый backend одновременно отдаёт статический frontend и API. Нужно решить, останется ли это так в Java или frontend будет отдельным сервисом.

## Что переносится без изменения контракта

- Публичные endpoints `/api/games`, `/api/rating`, `/api/gallery`.
- Форматы JSON для текущего frontend.
- Consent versions `1.0`.
- Правила показа только `published`/visible данных.
- Структура public uploads URL.
- Правило архивирования игр через 6 часов по умолчанию.

## Что нужно исправить при миграции

- Вынести всю бизнес-логику из Telegram handlers в Java services.
- Добавить полноценные роли `MASTER`, `CONTENT_MANAGER`, `RATING_MANAGER`, `SUPERADMIN`.
- Добавить строгую idempotency для всех rating mutations.
- Добавить OpenAPI.
- Добавить contract tests.
- Добавить Flyway baseline поверх существующей схемы.
- Добавить scheduled archive job.
- Не отдавать приватные Telegram/contact поля в public DTO.

## Рекомендуемая стратегия

1. Создать `apps/backend-java` рядом со старым backend.
2. Java backend подключить к той же PostgreSQL schema.
3. Начать с read-only public API совместимости.
4. Затем перенести заявки.
5. Затем перенести rating commands.
6. Затем галерею и media storage.
7. Затем Telegram bot workflows.
8. После smoke tests переключить Railway start command.
9. Старый Node backend удалить из production только после подтверждения.

## Матрица покрытия миграции на Java

Обновлено: 2026-07-22.

Статусы:

- `NOT_STARTED` — Java-аналога пока нет.
- `PARTIAL` — есть часть контроллера/сервиса, но не весь контракт, тесты или бизнес-правила.
- `MIGRATED` — есть DTO, валидация, сервис, repository/SQL, транзакции и сохранена базовая совместимость.
- `TESTED` — есть автоматические тесты, которые запускаются через Maven.
- `READY_FOR_SWITCH` — функциональность проверена на staging/production-like окружении и готова к переключению.

| Функция | Node endpoint или handler | Java endpoint или service | Статус | Тесты | Используется frontend | Используется Telegram-ботом | Комментарий |
|---|---|---|---|---|---|---|---|
| Health | `GET /health` в `src/api/server.ts` | `HealthController.health` | PARTIAL | Unit/MockMvc ещё нужны | Да | Railway/ops | Совместимый endpoint есть; нужно проверить latency и bot status на staging. |
| Readiness | `GET /ready` | `HealthController.ready` | PARTIAL | Unit/MockMvc ещё нужны | Нет | Railway/ops | Проверяет БД; Flyway readiness через actuator требует staging-проверки. |
| Афиша: список игр | `GET /api/games`, `listPublicGames` | `ScheduleController.listGames`, `ScheduleService.listPublicGames` | MIGRATED | Требуется integration test | Да | Нет | Добавлены фильтры, лимит/offset и совместимые поля старого DTO. |
| Афиша: одна игра | `GET /api/games/:id`, `getPublicGame` | `ScheduleController.getGame` | MIGRATED | Требуется integration test | Возможно | Нет | Возвращает только будущую `published` игру. |
| Алиас расписания | Нет отдельного Node endpoint | `GET /api/schedule` | MIGRATED | Требуется integration test | Потенциально | Нет | Совместимый алиас для будущего frontend. |
| Запись на игру | `POST /api/game-signups`, `createGameSignup` | `ScheduleController.createSignup`, `ScheduleService.createSignup` | PARTIAL | Consent unit есть; integration ещё нужен | Да | Нет | Сохраняет заявку, проверяет места/согласие, делает upsert и audit; добавлены Telegram-уведомления best-effort. Нужен тест на rollback/duplicates. |
| Заявка на услугу | `POST /api/service-requests`, `createServiceRequest` | `ServiceRequestController`, `ServiceRequestService` | PARTIAL | Consent unit есть; integration ещё нужен | Да | Нет | Сохраняет согласие и audit; добавлены Telegram-уведомления админам best-effort. |
| Публичные мастера | Прямого Node endpoint не было | `GET /api/masters` | PARTIAL | Требуется controller test | Потенциально | Нет | Возвращает активных мастеров без Telegram ID; профиль мастера ещё не перенесён полностью. |
| Публичный рейтинг | `GET /api/rating`, `listPublicRating` | `RatingController`, `RatingService` | MIGRATED | Требуется integration test | Да | Нет | Сортировка перенесена native SQL, среднее через `BigDecimal`. |
| Управление рейтингом | handlers в `src/bot/index.ts`, `src/services/rating.ts` | Нет Java mutation API | NOT_STARTED | Нет | Нет | Да | Нужно перенести создание игроков, начисления, batch, idempotency и историю. |
| Галерея: список | `GET /api/gallery`, `listPublicGalleryPosts` | `GalleryController.list`, `GalleryService.listPublicPosts` | PARTIAL | Требуется integration test | Да | Нет | Публичное чтение перенесено; prev/next и cover ещё не расширены. |
| Галерея: одна публикация | `GET /api/gallery/:publicId` | `GalleryController.get` | PARTIAL | Требуется integration test | Да | Нет | Возвращает историю и media; навигация соседних публикаций пока не добавлена. |
| Категории галереи | `GET /api/gallery/categories` | `GalleryController.categories` | MIGRATED | Требуется controller test | Возможно | Нет | Контракт сохранён. |
| Управление галереей | handlers `gallery:*` в `src/bot/index.ts`, `createGalleryPost` | Нет полного Java internal API | NOT_STARTED | Нет | Нет | Да | Нужно добавить create/update/media/publish/hide/restore/order. |
| Media pipeline | `storeTelegramGalleryImage`, `sharp` | `MediaStorage`, `LocalMediaStorage`, `MediaTypeDetector` | PARTIAL | `MediaTypeDetectorTest` | Косвенно | Да | Интерфейс, local storage и signature detection есть; resize medium/thumbnail и object storage implementation ещё нужны. |
| Internal create game | `POST /api/internal/games` | `InternalController.createGame` | MIGRATED | Требуется security/integration test | Нет | Может использовать бот | Создаёт игру с `AUTO_PUBLISH`, audit, статусом. |
| Internal master game edit | Частично в handlers Telegram-бота | `PATCH /api/internal/masters/{masterId}/games/{gameId}` | MIGRATED | Требуется security/integration test | Нет | Да | Обновляет только игру выбранного мастера, сохраняет audit `game.updated`. |
| Internal game status | `PATCH /api/internal/games/:id/status` | `PATCH /api/internal/games/{id}/status` | MIGRATED | Требуется security/integration test | Нет | Да | Статусы совместимы со старой БД. |
| Auto archive | `archivePastGames`, job, internal endpoint | `ScheduleService.archivePastGames`, `@Scheduled`, internal endpoint | PARTIAL | Требуется integration test | Косвенно | Ops | Java архивирует в `archived`, как Node; нужен тест по `dateTimeEnd` и 6h default. |
| Privacy withdrawal | `POST /api/internal/privacy/withdraw-consent` | `InternalService.withdrawConsent` | PARTIAL | Требуется integration test | Нет | Админ/ops | Есть update/anonymize и audit; нужна идемпотентность и точная стратегия audit retention. |
| Internal token security | Express middleware | `SecurityConfig`, `InternalApiTokenFilter` | TESTED | `InternalApiTokenFilterTest` | Нет | Да | Фильтр покрыт unit-тестом; нужен MockMvc тест всего security chain. |
| CORS | Express `cors` + `SITE_ORIGINS` | `WebConfig`, Spring Security CORS | PARTIAL | Требуется MockMvc test | Да | Нет | Поддерживает configured origins и `null` для `file://`. |
| DATABASE_URL parser | `src/config.ts` + Prisma | `DatabaseConfig.parseDatabaseUrl` | TESTED | `DatabaseConfigTest` | Нет | Railway | Покрыты Railway URL, JDBC URL и Prisma `schema` param. |
| Consent validation | `requireConsent` | `ConsentService` | TESTED | `ConsentServiceTest` | Да | Нет | Возвращает `CONSENT_REQUIRED` через Java exception handler. |
| Rate limit public POST | `src/api/rateLimit.ts` | `PublicPostRateLimitFilter` | TESTED | `PublicPostRateLimitFilterTest` | Да | Нет | In-memory; для горизонтального scaling нужен Redis/Bucket4j. |
| Telegram bot process | `src/bot/index.ts` | `apps/telegram-bot-java`, polling service | PARTIAL | `BotPropertiesTest` | Нет | Да | Java-процесс, health, polling и `/start` добавлены; Node-бот не отключать до переноса сценариев. |
| Bot sessions | `BotSession` через Prisma | Java service отсутствует | NOT_STARTED | Нет | Нет | Да | Нельзя переносить в static Map; нужен repository/service. |
| Notifications | `notifyTelegram`, `notifyAdmins` | `TelegramNotificationService` | PARTIAL | Требуется mock HTTP test | Нет | Косвенно | Best-effort отправка без логирования токенов. |
| Static frontend serving | Express static root | Java static serving не перенесён | NOT_STARTED | Нет | Да | Нет | Можно оставить frontend отдельным сервисом или добавить resource handler позже. |

## Решение по оставшимся Node-функциям

| Node-функция | Решение | Обоснование |
|---|---|---|
| `src/index.ts` общий запуск API+бота | Оставить временным адаптером | Нужен rollback, пока Java-бот не готов. |
| Express static frontend serving | Оставить временным адаптером или вынести frontend отдельно | Java API уже может жить отдельным Railway service. |
| `src/api/rateLimit.ts` | Перенести в Java | Базовая Java-версия добавлена, но нужен distributed вариант при масштабировании. |
| `src/services/games.ts` | Перенести в Java | Основной public/signup/internal контур перенесён; нужны тесты и edit API. |
| `src/services/serviceRequests.ts` | Перенести в Java | Базовый контур перенесён; нужны duplicate/idempotency tests. |
| `src/services/gallery.ts` | Перенести в Java | Public read частично перенесён; media pipeline и internal management ещё нет. |
| `src/services/rating.ts` | Перенести в Java | Public read перенесён; mutation и batch-операции ещё нет. |
| `src/services/privacy.ts` | Перенести в Java | Есть минимальная реализация; нужна расширенная идемпотентность. |
| `src/services/notifications.ts` | Перенести в Java | Базовый notification service добавлен. |
| `src/services/audit.ts` | Перенести в Java | Базовый append-only write добавлен. |
| `src/bot/index.ts` | Переписать отдельным Java bot service | До готовности Java-бота Node-бот не выключать. |
| `src/jobs/archivePastGames.ts` | Объединить с Java scheduled job/internal endpoint | Java scheduled job добавлен; smoke test обязателен. |
