CREATE TYPE "MasterRole" AS ENUM ('master', 'admin');
CREATE TYPE "MasterStatus" AS ENUM ('active', 'blocked');
CREATE TYPE "GameStatus" AS ENUM ('draft', 'pending', 'published', 'completed', 'cancelled', 'archived');
CREATE TYPE "SignupStatus" AS ENUM ('pending', 'confirmed', 'cancelled');
CREATE TYPE "ServiceRequestStatus" AS ENUM ('new', 'contacted', 'closed');
CREATE TYPE "ContactRequestStatus" AS ENUM ('new', 'contacted', 'closed', 'withdrawn');
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
CREATE TYPE "GalleryPostType" AS ENUM ('photo', 'story', 'character_sheet');
CREATE TYPE "GalleryCategory" AS ENUM ('games', 'events', 'heroes', 'tavern', 'miniatures', 'other');
CREATE TYPE "GalleryPostStatus" AS ENUM ('draft', 'published', 'hidden');

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

CREATE TABLE "GameSignup" (
  "id" TEXT NOT NULL,
  "gameId" TEXT NOT NULL,
  "masterId" TEXT NOT NULL,
  "playerName" TEXT NOT NULL,
  "contact" TEXT NOT NULL,
  "seats" INTEGER NOT NULL DEFAULT 1,
  "comment" TEXT,
  "status" "SignupStatus" NOT NULL DEFAULT 'confirmed',
  "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  "consentedAt" TIMESTAMP(3),
  "formType" TEXT NOT NULL DEFAULT 'legacy',
  "consentWithdrawnAt" TIMESTAMP(3),
  "dataUseStoppedAt" TIMESTAMP(3),
  "anonymizedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "GameSignup_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "ServiceRequest" (
  "id" TEXT NOT NULL,
  "name" TEXT NOT NULL,
  "contact" TEXT NOT NULL,
  "service" TEXT NOT NULL,
  "desiredDate" TEXT,
  "participants" INTEGER,
  "city" TEXT,
  "comment" TEXT,
  "status" "ServiceRequestStatus" NOT NULL DEFAULT 'new',
  "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  "consentedAt" TIMESTAMP(3),
  "formType" TEXT NOT NULL DEFAULT 'legacy',
  "consentWithdrawnAt" TIMESTAMP(3),
  "dataUseStoppedAt" TIMESTAMP(3),
  "anonymizedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "ServiceRequest_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "ContactRequest" (
  "id" TEXT NOT NULL,
  "name" TEXT NOT NULL,
  "contact" TEXT NOT NULL,
  "topic" TEXT,
  "comment" TEXT NOT NULL,
  "status" "ContactRequestStatus" NOT NULL DEFAULT 'new',
  "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  "consentedAt" TIMESTAMP(3),
  "formType" TEXT NOT NULL DEFAULT 'legacy',
  "consentWithdrawnAt" TIMESTAMP(3),
  "dataUseStoppedAt" TIMESTAMP(3),
  "anonymizedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "ContactRequest_pkey" PRIMARY KEY ("id")
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

CREATE TABLE "GalleryPost" (
  "id" TEXT NOT NULL,
  "publicId" TEXT NOT NULL,
  "type" "GalleryPostType" NOT NULL DEFAULT 'photo',
  "title" TEXT NOT NULL,
  "description" TEXT,
  "storyContent" TEXT,
  "storyHtml" TEXT,
  "category" "GalleryCategory" NOT NULL DEFAULT 'games',
  "eventDate" TIMESTAMP(3),
  "authorMasterId" TEXT,
  "status" "GalleryPostStatus" NOT NULL DEFAULT 'draft',
  "isVisible" BOOLEAN NOT NULL DEFAULT true,
  "sortOrder" INTEGER NOT NULL DEFAULT 0,
  "publishedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "GalleryPost_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "GalleryMedia" (
  "id" TEXT NOT NULL,
  "galleryPostId" TEXT NOT NULL,
  "fileUrl" TEXT NOT NULL,
  "thumbnailUrl" TEXT NOT NULL,
  "mediumUrl" TEXT NOT NULL,
  "width" INTEGER,
  "height" INTEGER,
  "mimeType" TEXT NOT NULL,
  "altText" TEXT,
  "sortOrder" INTEGER NOT NULL DEFAULT 0,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "GalleryMedia_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "Master_telegramUserId_key" ON "Master"("telegramUserId");
CREATE INDEX "Master_status_idx" ON "Master"("status");
CREATE INDEX "Master_role_idx" ON "Master"("role");
CREATE INDEX "Game_status_dateTimeStart_idx" ON "Game"("status", "dateTimeStart");
CREATE INDEX "Game_masterId_status_idx" ON "Game"("masterId", "status");
CREATE INDEX "Game_dateTimeStart_idx" ON "Game"("dateTimeStart");
CREATE UNIQUE INDEX "GameSignup_gameId_contact_key" ON "GameSignup"("gameId", "contact");
CREATE INDEX "GameSignup_gameId_status_idx" ON "GameSignup"("gameId", "status");
CREATE INDEX "GameSignup_masterId_status_idx" ON "GameSignup"("masterId", "status");
CREATE INDEX "GameSignup_createdAt_idx" ON "GameSignup"("createdAt");
CREATE INDEX "GameSignup_consentGiven_idx" ON "GameSignup"("consentGiven");
CREATE INDEX "ServiceRequest_service_status_idx" ON "ServiceRequest"("service", "status");
CREATE INDEX "ServiceRequest_createdAt_idx" ON "ServiceRequest"("createdAt");
CREATE INDEX "ServiceRequest_consentGiven_idx" ON "ServiceRequest"("consentGiven");
CREATE INDEX "ContactRequest_status_idx" ON "ContactRequest"("status");
CREATE INDEX "ContactRequest_consentGiven_idx" ON "ContactRequest"("consentGiven");
CREATE INDEX "ContactRequest_createdAt_idx" ON "ContactRequest"("createdAt");
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
CREATE INDEX "AuditLog_entityType_entityId_idx" ON "AuditLog"("entityType", "entityId");
CREATE INDEX "AuditLog_createdAt_idx" ON "AuditLog"("createdAt");
CREATE UNIQUE INDEX "GalleryPost_publicId_key" ON "GalleryPost"("publicId");
CREATE INDEX "GalleryPost_status_isVisible_sortOrder_createdAt_idx" ON "GalleryPost"("status", "isVisible", "sortOrder", "createdAt");
CREATE INDEX "GalleryPost_category_status_idx" ON "GalleryPost"("category", "status");
CREATE INDEX "GalleryPost_authorMasterId_idx" ON "GalleryPost"("authorMasterId");
CREATE INDEX "GalleryMedia_galleryPostId_sortOrder_idx" ON "GalleryMedia"("galleryPostId", "sortOrder");

ALTER TABLE "Game" ADD CONSTRAINT "Game_masterId_fkey" FOREIGN KEY ("masterId") REFERENCES "Master"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "GameSignup" ADD CONSTRAINT "GameSignup_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "GameSignup" ADD CONSTRAINT "GameSignup_masterId_fkey" FOREIGN KEY ("masterId") REFERENCES "Master"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "RatingEvent" ADD CONSTRAINT "RatingEvent_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "RatingPlayer"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "RatingEvent" ADD CONSTRAINT "RatingEvent_playedGameId_fkey" FOREIGN KEY ("playedGameId") REFERENCES "RatingPlayedGame"("id") ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE "GalleryPost" ADD CONSTRAINT "GalleryPost_authorMasterId_fkey" FOREIGN KEY ("authorMasterId") REFERENCES "Master"("id") ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE "GalleryMedia" ADD CONSTRAINT "GalleryMedia_galleryPostId_fkey" FOREIGN KEY ("galleryPostId") REFERENCES "GalleryPost"("id") ON DELETE CASCADE ON UPDATE CASCADE;
