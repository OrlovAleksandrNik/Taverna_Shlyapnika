CREATE TYPE "MasterRole" AS ENUM ('master', 'admin');
CREATE TYPE "MasterStatus" AS ENUM ('active', 'blocked');
CREATE TYPE "GameStatus" AS ENUM ('draft', 'pending', 'published', 'completed', 'cancelled', 'archived');

CREATE TABLE "Master" (
  "id" TEXT NOT NULL,
  "telegramUserId" BIGINT NOT NULL,
  "telegramUsername" TEXT,
  "displayName" TEXT NOT NULL,
  "contactUrl" TEXT NOT NULL,
  "role" "MasterRole" NOT NULL DEFAULT 'master',
  "status" "MasterStatus" NOT NULL DEFAULT 'active',
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "Master_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "Game" (
  "id" TEXT NOT NULL,
  "masterId" TEXT NOT NULL,
  "title" TEXT NOT NULL,
  "description" TEXT NOT NULL,
  "gameSystem" TEXT NOT NULL,
  "experienceLevel" TEXT NOT NULL,
  "ageRating" TEXT NOT NULL,
  "dateTimeStart" TIMESTAMP(3) NOT NULL,
  "durationMinutes" INTEGER,
  "dateTimeEnd" TIMESTAMP(3),
  "minPlayers" INTEGER NOT NULL,
  "maxPlayers" INTEGER NOT NULL,
  "price" DECIMAL(10,2) NOT NULL,
  "currency" TEXT NOT NULL DEFAULT 'BYN',
  "imageUrl" TEXT,
  "contactUrl" TEXT NOT NULL,
  "status" "GameStatus" NOT NULL DEFAULT 'draft',
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  "publishedAt" TIMESTAMP(3),
  "completedAt" TIMESTAMP(3),
  "cancelledAt" TIMESTAMP(3),
  CONSTRAINT "Game_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "AuditLog" (
  "id" TEXT NOT NULL,
  "userId" TEXT,
  "action" TEXT NOT NULL,
  "entityType" TEXT NOT NULL,
  "entityId" TEXT,
  "details" JSONB,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "AuditLog_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "BotSession" (
  "telegramUserId" BIGINT NOT NULL,
  "state" TEXT NOT NULL,
  "draft" JSONB,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "BotSession_pkey" PRIMARY KEY ("telegramUserId")
);

CREATE UNIQUE INDEX "Master_telegramUserId_key" ON "Master"("telegramUserId");
CREATE INDEX "Master_status_idx" ON "Master"("status");
CREATE INDEX "Master_role_idx" ON "Master"("role");
CREATE INDEX "Game_status_dateTimeStart_idx" ON "Game"("status", "dateTimeStart");
CREATE INDEX "Game_masterId_status_idx" ON "Game"("masterId", "status");
CREATE INDEX "Game_dateTimeStart_idx" ON "Game"("dateTimeStart");
CREATE INDEX "AuditLog_entityType_entityId_idx" ON "AuditLog"("entityType", "entityId");
CREATE INDEX "AuditLog_createdAt_idx" ON "AuditLog"("createdAt");

ALTER TABLE "Game" ADD CONSTRAINT "Game_masterId_fkey" FOREIGN KEY ("masterId") REFERENCES "Master"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
