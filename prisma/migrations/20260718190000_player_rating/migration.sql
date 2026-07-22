CREATE TYPE "RatingEventType" AS ENUM (
  'player_created',
  'player_updated',
  'player_hidden',
  'player_shown',
  'game_result',
  'points_adjustment',
  'inspiration_adjustment',
  'correction'
);

CREATE TABLE "RatingPlayer" (
  "id" TEXT NOT NULL,
  "displayName" TEXT NOT NULL,
  "nickname" TEXT,
  "avatarUrl" TEXT,
  "isVisible" BOOLEAN NOT NULL DEFAULT true,
  "gamesPlayed" INTEGER NOT NULL DEFAULT 0,
  "totalPoints" INTEGER NOT NULL DEFAULT 0,
  "inspirationCount" INTEGER NOT NULL DEFAULT 0,
  "lastGameAt" TIMESTAMP(3),
  "lastStatsAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "RatingPlayer_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "RatingPlayedGame" (
  "id" TEXT NOT NULL,
  "title" TEXT NOT NULL,
  "gameDate" TIMESTAMP(3),
  "masterName" TEXT,
  "notes" TEXT,
  "createdByTelegramId" BIGINT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "RatingPlayedGame_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "RatingEvent" (
  "id" TEXT NOT NULL,
  "playerId" TEXT NOT NULL,
  "playedGameId" TEXT,
  "type" "RatingEventType" NOT NULL,
  "pointsDelta" INTEGER NOT NULL DEFAULT 0,
  "inspirationDelta" INTEGER NOT NULL DEFAULT 0,
  "gamesDelta" INTEGER NOT NULL DEFAULT 0,
  "reason" TEXT NOT NULL,
  "createdByTelegramId" BIGINT,
  "createdByMasterId" TEXT,
  "reversalOfEventId" TEXT,
  "idempotencyKey" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "RatingEvent_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "RatingEvent_idempotencyKey_key" ON "RatingEvent"("idempotencyKey");
CREATE INDEX "RatingPlayer_isVisible_idx" ON "RatingPlayer"("isVisible");
CREATE INDEX "RatingPlayer_totalPoints_gamesPlayed_idx" ON "RatingPlayer"("totalPoints", "gamesPlayed");
CREATE INDEX "RatingPlayer_displayName_idx" ON "RatingPlayer"("displayName");
CREATE INDEX "RatingPlayedGame_gameDate_idx" ON "RatingPlayedGame"("gameDate");
CREATE INDEX "RatingPlayedGame_createdAt_idx" ON "RatingPlayedGame"("createdAt");
CREATE INDEX "RatingEvent_playerId_createdAt_idx" ON "RatingEvent"("playerId", "createdAt");
CREATE INDEX "RatingEvent_playedGameId_idx" ON "RatingEvent"("playedGameId");
CREATE INDEX "RatingEvent_type_idx" ON "RatingEvent"("type");
CREATE INDEX "RatingEvent_createdAt_idx" ON "RatingEvent"("createdAt");

ALTER TABLE "RatingEvent" ADD CONSTRAINT "RatingEvent_playerId_fkey"
  FOREIGN KEY ("playerId") REFERENCES "RatingPlayer"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "RatingEvent" ADD CONSTRAINT "RatingEvent_playedGameId_fkey"
  FOREIGN KEY ("playedGameId") REFERENCES "RatingPlayedGame"("id") ON DELETE SET NULL ON UPDATE CASCADE;
