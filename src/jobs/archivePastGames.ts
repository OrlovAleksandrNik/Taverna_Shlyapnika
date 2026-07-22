import { archivePastGames } from "../services/games.js";
import { prisma } from "../db.js";
import { logger } from "../logger.js";

const archived = await archivePastGames();
logger.info({ archived }, "past games archive finished");
await prisma.$disconnect();
