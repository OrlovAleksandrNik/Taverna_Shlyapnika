# Control Service Audit

Дата аудита: 2026-07-24.

## Найденные проекты

| Проект | Расположение | Стек | Отдельный запуск | Встраивание как модуль | Точки входа | Риски интеграции |
| --- | --- | --- | --- | --- | --- | --- |
| Основной сайт | `Taverna-Shlyapnika/` | Static HTML/CSS/JS, TypeScript Node API, Prisma, PostgreSQL | Да, через Node scripts и Docker | Только через API/события, не копированием страниц | `index.html`, `script.js`, `src/index.ts`, `src/api/server.ts` | Есть production-схема Prisma; нельзя подключать control-service к этой БД напрямую |
| Java backend сайта | `Taverna-Shlyapnika/apps/backend-java/` | Java 21, Spring Boot 3.3.7, JPA, Flyway, PostgreSQL, OpenAPI | Да, Maven wrapper, Docker | Можно как отдельный сервис за API Gateway | `TavernaApplication`, `/api/internal/**`, `/actuator/health/**` | Нельзя вызывать из control-service на текущем этапе; внутренние токены не копировать |
| Старый Node backend | `Taverna-Shlyapnika/src/api/` | Express, TypeScript, Prisma | Да, `pnpm dev:backend` | Лучше заменить контрактом Java backend или internal gateway | `src/api/server.ts` | CORS/rate limit надо держать отдельно; не использовать как shared admin backend |
| Telegram bot | `Taverna-Shlyapnika/src/bot/`, `Taverna-Shlyapnika/apps/telegram-bot-java/` | Grammy/TypeScript и Java Spring Boot bot client | Да | Только через backend contracts, не через общую БД | `src/bot/index.ts`, `TavernaTelegramBotApplication` | Нельзя менять бот и переносить Telegram IDs в кабинет без permission |
| VoiceMod | `../voicemod-panel-d-work/` | Node.js ESM server, static HTML/CSS/JS, local audio library | Да, `node server.mjs` | Представлять как managed project; запуск только через будущий Desktop Agent | `server.mjs`, `index.html`, `app.js` | Текущий server разрешает `Access-Control-Allow-Origin: *`; не запускать из браузера админки |
| ScreenStage | `../ScreenStage-redesign/` | .NET 8 WPF, Windows Forms interop, LibVLCSharp | Да, desktop executable | Представлять как managed project; без физического объединения | `ScreenStage.csproj`, `Program.cs`, `MainWindow.cs`, `bin/Current/ScreenStage.exe` | Нельзя принимать путь запуска от frontend; нужен Desktop Agent с allowlist |

## Дизайн

Переиспользуемые элементы:

- Палитра из `styles.css`: `--bg`, `--surface`, `--paper`, `--green`, `--brass`, `--copper`.
- Фон `assets/hero-tavern.png`.
- Брендовый знак `assets/images/brand/hatter-mark.png`.
- Декоративные шрифты `assets/fonts/*`, только для заголовков.
- UI-паттерны: 8px radius, латунные primary buttons, тёмные панели, пергаментные поверхности.

Не переиспользовать напрямую:

- Навигацию публичного сайта как скрытый admin route.
- Production Prisma schema как JPA-модель control-service.
- Telegram-specific user identity как основную identity model.

## Модели и endpoint-контракты

Существующие домены основного проекта: `Master`, `Game`, `GameSignup`, `GalleryPost`, `GalleryMedia`, `ServiceRequest`, `ContactRequest`, `RatingPlayer`, `RatingPlayedGame`, `RatingEvent`, `AuditLog`, `BotSession`.

Текущие backend endpoints видны в:

- `src/api/server.ts`
- `apps/backend-java/src/main/java/by/taverna/shlyapnika/internal/api/InternalController.java`
- `apps/backend-java/src/main/java/by/taverna/shlyapnika/schedule/api/ScheduleController.java`
- `apps/backend-java/src/main/java/by/taverna/shlyapnika/gallery/api/GalleryController.java`
- `apps/backend-java/src/main/java/by/taverna/shlyapnika/rating/api/RatingController.java`

Для control-service подготовлен отдельный namespace `/api/v1/**`; прямых JPA-связей с основной БД нет.
