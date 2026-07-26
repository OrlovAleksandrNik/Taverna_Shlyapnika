CREATE TYPE "MasterAccessRequestStatus" AS ENUM ('pending', 'approved', 'rejected');

CREATE TABLE "MasterAccessRequest" (
  "id" TEXT NOT NULL,
  "displayName" TEXT NOT NULL,
  "email" TEXT NOT NULL,
  "telegramUsername" TEXT NOT NULL,
  "normalizedTelegramUsername" TEXT NOT NULL,
  "requestedRole" TEXT NOT NULL DEFAULT 'master',
  "status" "MasterAccessRequestStatus" NOT NULL DEFAULT 'pending',
  "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  "consentedAt" TIMESTAMP(3),
  "formType" TEXT NOT NULL DEFAULT 'master-registration',
  "decidedAt" TIMESTAMP(3),
  "decidedByTelegramId" BIGINT,
  "decisionComment" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "MasterAccessRequest_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "MasterAccessRequest_status_createdAt_idx" ON "MasterAccessRequest" ("status", "createdAt");
CREATE INDEX "MasterAccessRequest_normalizedTelegramUsername_idx" ON "MasterAccessRequest" ("normalizedTelegramUsername");
