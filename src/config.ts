import "dotenv/config";
import { z } from "zod";

const normalizeUrl = (fallback: string) =>
  z.preprocess((value) => {
    if (typeof value !== "string") {
      return fallback;
    }

    const trimmed = value.trim();
    if (!trimmed || trimmed.includes("${{") || trimmed === "https://" || trimmed.startsWith("https:///") || trimmed.startsWith("http:///")) {
      return fallback;
    }

    return trimmed;
  }, z.string().url().default(fallback));

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
  SITE_BASE_URL: normalizeUrl("http://localhost:4177"),
  SITE_ORIGINS: z.string().optional().default("http://localhost:4177"),
  TAVERN_TIMEZONE: z.string().default("Europe/Minsk"),
  PUBLIC_UPLOADS_URL: normalizeUrl("http://localhost:4177/uploads"),
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
  .filter((value) => {
    if (!value || value.includes("${{") || value === "https://") {
      return false;
    }

    try {
      new URL(value);
      return true;
    } catch {
      return false;
    }
  });
