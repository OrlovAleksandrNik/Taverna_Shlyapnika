import { config, databaseConfigured } from "./config.js";
import { startApi } from "./api/server.js";
import { startBot } from "./bot/index.js";
import { archivePastGames } from "./services/games.js";
import { logger } from "./logger.js";

logger.info(
  {
    mode: config.BOT_MODE,
    port: config.PORT,
    botEnabled: Boolean(config.TELEGRAM_BOT_TOKEN) && databaseConfigured,
    databaseConfigured
  },
  "taverna process starting"
);

startApi();

if (config.TELEGRAM_BOT_TOKEN && databaseConfigured) {
  await startBot();
} else if (config.TELEGRAM_BOT_TOKEN && !databaseConfigured) {
  logger.warn("DATABASE_URL is empty: Telegram bot is disabled because it requires the database");
} else {
  logger.warn("TELEGRAM_BOT_TOKEN is empty: API and site are running without Telegram bot");
}

if (databaseConfigured) {
  setInterval(() => {
    archivePastGames().catch((error) => logger.error({ error }, "auto archive failed"));
  }, 10 * 60 * 1000);
} else {
  logger.warn("DATABASE_URL is empty: automatic game archiving is disabled");
}

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.on(signal, () => {
    logger.info({ signal }, "taverna process stopping");
    process.exit(0);
  });
}

process.on("unhandledRejection", (error) => {
  logger.error({ error: error instanceof Error ? { name: error.name, message: error.message } : String(error) }, "unhandled rejection");
});

process.on("uncaughtException", (error) => {
  logger.error({ error: { name: error.name, message: error.message } }, "uncaught exception");
  process.exit(1);
});
