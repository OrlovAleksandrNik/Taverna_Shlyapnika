CREATE TABLE "SiteAccount" (
  "id" TEXT NOT NULL,
  "displayName" TEXT NOT NULL,
  "email" TEXT NOT NULL,
  "passwordHash" TEXT NOT NULL,
  "role" TEXT NOT NULL,
  "status" TEXT NOT NULL DEFAULT 'active',
  "telegramUsername" TEXT,
  "normalizedTelegramUsername" TEXT,
  "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  "consentedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "SiteAccount_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "SiteAccount_email_key" ON "SiteAccount"("email");
CREATE INDEX "SiteAccount_role_status_idx" ON "SiteAccount"("role", "status");
CREATE INDEX "SiteAccount_normalizedTelegramUsername_idx" ON "SiteAccount"("normalizedTelegramUsername");
