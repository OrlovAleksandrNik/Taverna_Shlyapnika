ALTER TABLE "SiteAccount"
  ADD COLUMN IF NOT EXISTS "telegramUserId" BIGINT;

CREATE INDEX IF NOT EXISTS "SiteAccount_telegramUserId_idx"
  ON "SiteAccount"("telegramUserId");

-- Data repair: the existing owner account was registered as master Alexander.
-- The stable Telegram user_id is now stored on the account; username/email are used
-- only once to find the already-created record without creating a duplicate login.
UPDATE "SiteAccount"
SET "role" = 'hatter',
    "status" = 'active',
    "telegramUserId" = 1169106804,
    "updatedAt" = CURRENT_TIMESTAMP
WHERE "telegramUserId" IS DISTINCT FROM 1169106804
  AND (
    lower(coalesce("normalizedTelegramUsername", '')) = 'misterhatter'
    OR lower("email") = 'alec202327@gmail.com'
  );
