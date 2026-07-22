CREATE TYPE "SignupStatus" AS ENUM ('pending', 'confirmed', 'cancelled');

CREATE TYPE "ServiceRequestStatus" AS ENUM ('new', 'contacted', 'closed');

CREATE TABLE "GameSignup" (
  "id" TEXT NOT NULL,
  "gameId" TEXT NOT NULL,
  "masterId" TEXT NOT NULL,
  "playerName" TEXT NOT NULL,
  "contact" TEXT NOT NULL,
  "seats" INTEGER NOT NULL DEFAULT 1,
  "comment" TEXT,
  "status" "SignupStatus" NOT NULL DEFAULT 'confirmed',
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
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,

  CONSTRAINT "ServiceRequest_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "GameSignup_gameId_contact_key" ON "GameSignup"("gameId", "contact");
CREATE INDEX "GameSignup_gameId_status_idx" ON "GameSignup"("gameId", "status");
CREATE INDEX "GameSignup_masterId_status_idx" ON "GameSignup"("masterId", "status");
CREATE INDEX "GameSignup_createdAt_idx" ON "GameSignup"("createdAt");
CREATE INDEX "ServiceRequest_service_status_idx" ON "ServiceRequest"("service", "status");
CREATE INDEX "ServiceRequest_createdAt_idx" ON "ServiceRequest"("createdAt");

ALTER TABLE "GameSignup" ADD CONSTRAINT "GameSignup_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "GameSignup" ADD CONSTRAINT "GameSignup_masterId_fkey" FOREIGN KEY ("masterId") REFERENCES "Master"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
