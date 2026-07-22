# API module

Этот модуль содержит Express API и раздачу статического сайта.

Главный файл: `server.ts`.

Смежная логика вынесена в:

- `../services/games.ts`
- `../services/serviceRequests.ts`
- `../services/rating.ts`
- `../services/privacy.ts`
- `../services/consent.ts`
- `rateLimit.ts`

Не добавляйте бизнес-логику прямо в маршруты, если её можно вынести в сервис.
