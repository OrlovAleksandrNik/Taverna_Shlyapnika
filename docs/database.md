# База данных

Проект использует PostgreSQL и Prisma. Схема находится в `prisma/schema.prisma`, миграции — в `prisma/migrations`.

## Master

Мастер Telegram-бота.

Ключевые поля: `telegramUserId`, `telegramUsername`, `displayName`, `contactUrl`, `role`, `status`.

Связи: один мастер создаёт много игр и получает заявки на эти игры.

## Game

Игра в афише.

Ключевые поля: `title`, `description`, `gameSystem`, `experienceLevel`, `ageRating`, `dateTimeStart`, `dateTimeEnd`, `minPlayers`, `maxPlayers`, `price`, `currency`, `imageUrl`, `contactUrl`, `status`.

Активная афиша показывает только `published` будущие игры. Прошедшие игры не удаляются физически, а переводятся в `archived`.

## GameSignup

Заявка игрока на конкретную игру.

Ключевые поля: `gameId`, `masterId`, `playerName`, `contact`, `seats`, `comment`, `status`, поля согласия.

Ограничение: `gameId + contact` уникальны, чтобы не создавать дубликаты одной записи.

## ServiceRequest

Заявка на услугу.

Ключевые поля: `name`, `contact`, `service`, `desiredDate`, `participants`, `city`, `comment`, `status`, поля согласия.

## ContactRequest

Историческая модель для обратной связи. Контактная форма с сайта удалена, но модель оставлена для совместимости и обработки уже существующих записей.

## RatingPlayer

Публичный игрок рейтинга.

Ключевые поля: `displayName`, `nickname`, `avatarUrl`, `isVisible`, `gamesPlayed`, `totalPoints`, `inspirationCount`, `lastGameAt`.

## RatingPlayedGame

Сыгранная игра для истории рейтинга.

Ключевые поля: `title`, `gameDate`, `masterName`, `notes`, `createdByTelegramId`.

## RatingEvent

Журнал изменений рейтинга.

Ключевые поля: `playerId`, `playedGameId`, `type`, `pointsDelta`, `inspirationDelta`, `gamesDelta`, `reason`, `idempotencyKey`.

`idempotencyKey` защищает отдельные операции от повторной записи, если он передан обработчиком.

## AuditLog

Технический журнал действий.

Ключевые поля: `userId`, `action`, `entityType`, `entityId`, `details`, `createdAt`.

## BotSession

Состояния Telegram-диалогов.

Ключевые поля: `telegramUserId`, `state`, `draft`.

## Правила хранения

- Не коммитить production-базу и дампы.
- Не удалять прошедшие игры физически.
- Персональные данные заявок удалять или обезличивать через privacy-операцию при отзыве согласия.
- Все структурные изменения базы проводить миграциями Prisma.
