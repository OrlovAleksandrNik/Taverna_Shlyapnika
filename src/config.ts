import "dotenv/config";
import { z } from "zod";

const schema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().positive().default(4177),
  DATABASE_URL: z.string().optional().default(""),
  TELEGRAM_BOT_TOKEN: z.string().optional().default(""),
  BOT_MODE: z.enum(["polling", "webhook"]).default("polling"),
  WEBHOOK_URL: z.string().url().optional().or(z.literal("")).default(""),
  INTERNAL_API_TOKEN: z.string().min(12).default("change-me-local-token"),
  ADMIN_TELEGRAM_IDS: z.string().optional().default(""),
  AUTO_PUBLISH: z.coerce.boolean().default(true),
  SITE_BASE_URL: z.string().url().default("http://localhost:4177"),
  SITE_ORIGINS: z.string().optional().default("http://localhost:4177"),
  TAVERN_TIMEZONE: z.string().default("Europe/Minsk"),
  PUBLIC_UPLOADS_URL: z.string().url().default("http://localhost:4177/uploads"),
  FILE_STORAGE_DIR: z.string().default("uploads")
});

export const config = schema.parse(process.env);

export const databaseConfigured = Boolean(config.DATABASE_URL);

export const adminTelegramIds = new Set(
  config.ADMIN_TELEGRAM_IDS.split(",")
    .map((value) => value.trim())
    .filter(Boolean)
);

export const siteOrigins = config.SITE_ORIGINS.split(",")
  .map((value) => value.trim())
  .filter(Boolean);
