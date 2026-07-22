CREATE TYPE "ContactRequestStatus" AS ENUM ('new', 'contacted', 'closed', 'withdrawn');

ALTER TABLE "GameSignup"
  ADD COLUMN "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN "consentedAt" TIMESTAMP(3),
  ADD COLUMN "formType" TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN "consentWithdrawnAt" TIMESTAMP(3),
  ADD COLUMN "dataUseStoppedAt" TIMESTAMP(3),
  ADD COLUMN "anonymizedAt" TIMESTAMP(3);

ALTER TABLE "ServiceRequest"
  ADD COLUMN "consentGiven" BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN "consentVersion" TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN "privacyPolicyVersion" TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN "consentedAt" TIMESTAMP(3),
  ADD COLUMN "formType" TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN "consentWithdrawnAt" TIMESTAMP(3),
  ADD COLUMN "dataUseStoppedAt" TIMESTAMP(3),
  ADD COLUMN "anonymizedAt" TIMESTAMP(3);

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

CREATE INDEX "GameSignup_consentGiven_idx" ON "GameSignup"("consentGiven");
CREATE INDEX "ServiceRequest_consentGiven_idx" ON "ServiceRequest"("consentGiven");
CREATE INDEX "ContactRequest_status_idx" ON "ContactRequest"("status");
CREATE INDEX "ContactRequest_consentGiven_idx" ON "ContactRequest"("consentGiven");
CREATE INDEX "ContactRequest_createdAt_idx" ON "ContactRequest"("createdAt");
