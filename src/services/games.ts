import { prisma } from "../db.js";
import { logger } from "../logger.js";
import { formatForSite } from "../utils/time.js";
import { audit } from "./audit.js";
import { ConsentInput, requireConsent } from "./consent.js";
import { notifyAdmins, notifyTelegram } from "./notifications.js";

type GameStatus = "draft" | "pending" | "published" | "completed" | "cancelled" | "archived";

const DEFAULT_GAME_DURATION_MS = 6 * 60 * 60 * 1000;

function confirmedSeats(game: any) {
  return Array.isArray(game.signups)
    ? game.signups.reduce((sum: number, signup: any) => sum + Number(signup.seats || 0), 0)
    : 0;
}

function isNewbieExperience(value: string) {
  const normalized = value.toLowerCase();
  return normalized.includes("нов") || normalized.includes("без") || normalized.includes("нач");
}

function isDndSystem(value: string) {
  const normalized = value.toLowerCase();
  return normalized.includes("d&d") || normalized.includes("dnd") || normalized.includes("dungeons");
}

export async function archivePastGames() {
  const now = new Date();
  const defaultEnd = new Date(now.getTime() - DEFAULT_GAME_DURATION_MS);
  const result = await prisma.game.updateMany({
    where: {
      status: "published",
      OR: [
        { dateTimeEnd: { lt: now } },
        { dateTimeEnd: null, dateTimeStart: { lt: defaultEnd } }
      ]
    },
    data: {
      status: "archived",
      completedAt: now
    }
  });

  if (result.count > 0) {
    await audit(undefined, "games.auto_archived", "Game", undefined, { count: result.count });
    logger.info({ count: result.count }, "past games archived");
  }

  return result.count;
}

const publicGameInclude = {
  master: true,
  signups: {
    where: { status: "confirmed" },
    select: { seats: true }
  }
} as const;

export async function listPublicGames(query: {
  dateFrom?: Date;
  dateTo?: Date;
  masterId?: string;
  system?: string;
  limit?: number;
  offset?: number;
}) {
  await archivePastGames();
  const now = new Date();
  const dateFrom = query.dateFrom && query.dateFrom > now ? query.dateFrom : now;

  const where: Record<string, unknown> = {
    status: "published",
    dateTimeStart: { gte: dateFrom }
  };

  if (query.dateTo) {
    where.dateTimeStart = { ...(where.dateTimeStart as object), lte: query.dateTo };
  }

  if (query.masterId) where.masterId = query.masterId;
  if (query.system) where.gameSystem = { contains: query.system, mode: "insensitive" };

  const games = await prisma.game.findMany({
    where: where as any,
    include: publicGameInclude,
    orderBy: { dateTimeStart: "asc" },
    take: Math.min(query.limit || 30, 100),
    skip: query.offset || 0
  });

  logger.info({ count: games.length }, "public games requested");
  return games.map(publicGameDto);
}

export async function getPublicGame(id: string) {
  await archivePastGames();
  const game = await prisma.game.findFirst({
    where: { id, status: "published", dateTimeStart: { gte: new Date() } },
    include: publicGameInclude
  });

  return game ? publicGameDto(game) : null;
}

export function publicGameDto(game: any) {
  const bookedSeats = confirmedSeats(game);
  const availableSeats = Math.max(Number(game.maxPlayers || 0) - bookedSeats, 0);

  return {
    id: game.id,
    title: game.title,
    description: game.description,
    system: game.gameSystem,
    gameSystem: game.gameSystem,
    experienceLevel: game.experienceLevel,
    ageRating: game.ageRating,
    dateTimeStart: game.dateTimeStart.toISOString(),
    dateTimeEnd: game.dateTimeEnd?.toISOString() || null,
    startsAtLabel: formatForSite(game.dateTimeStart),
    durationMinutes: game.durationMinutes,
    minPlayers: game.minPlayers,
    maxPlayers: game.maxPlayers,
    bookedSeats,
    availableSeats,
    price: game.price.toString(),
    currency: game.currency,
    contactUrl: game.contactUrl,
    status: game.status,
    masterId: game.master.id,
    masterName: game.master.displayName,
    masterTelegramUsername: game.master.telegramUsername,
    systemName: game.gameSystem,
    master: {
      id: game.master.id,
      name: game.master.displayName,
      telegramUsername: game.master.telegramUsername
    },
    tags: [
      isDndSystem(game.gameSystem) ? "dnd" : "other",
      isNewbieExperience(game.experienceLevel) ? "newbie" : "experienced"
    ]
  };
}

export async function createGameSignup(input: {
  gameId: string;
  playerName: string;
  contact: string;
  seats: number;
  comment?: string;
  consent: ConsentInput;
}) {
  await archivePastGames();
  const consent = requireConsent(input.consent, "game-booking");

  const game = await prisma.game.findFirst({
    where: { id: input.gameId, status: "published", dateTimeStart: { gte: new Date() } },
    include: publicGameInclude
  });

  if (!game) throw new Error("Игра не найдена или уже недоступна для записи.");

  const availableSeats = Math.max(game.maxPlayers - confirmedSeats(game), 0);
  if (input.seats > availableSeats) {
    throw new Error("На эту игру уже нет нужного количества свободных мест.");
  }

  const signup = await prisma.gameSignup.upsert({
    where: {
      gameId_contact: {
        gameId: game.id,
        contact: input.contact
      }
    },
    create: {
      gameId: game.id,
      masterId: game.masterId,
      playerName: input.playerName,
      contact: input.contact,
      seats: input.seats,
      comment: input.comment,
      status: "confirmed",
      ...consent
    },
    update: {
      playerName: input.playerName,
      seats: input.seats,
      comment: input.comment,
      status: "confirmed",
      ...consent
    }
  });

  await audit(undefined, "game.signup", "GameSignup", signup.id, {
    gameId: game.id,
    seats: input.seats,
    consentVersion: consent.consentVersion,
    privacyPolicyVersion: consent.privacyPolicyVersion
  });

  const message = [
    "Новая запись на игру",
    "",
    `Игра: ${game.title}`,
    `Дата: ${formatForSite(game.dateTimeStart)}`,
    `Игрок: ${input.playerName}`,
    `Контакт: ${input.contact}`,
    `Мест: ${input.seats}`,
    input.comment ? `Комментарий: ${input.comment}` : ""
  ].filter(Boolean).join("\n");

  await Promise.all([
    notifyTelegram(game.master.telegramUserId, message),
    notifyAdmins(message)
  ]);

  logger.info({ gameId: game.id, signupId: signup.id, seats: input.seats }, "game signup saved");
  return { signup, game: publicGameDto({ ...game, signups: [...game.signups, { seats: input.seats }] }) };
}

export async function setGameStatus(gameId: string, status: GameStatus, userId?: string) {
  const data: Record<string, unknown> = { status };
  if (status === "published") data.publishedAt = new Date();
  if (status === "cancelled") data.cancelledAt = new Date();
  if (status === "completed" || status === "archived") data.completedAt = new Date();

  const game = await prisma.game.update({ where: { id: gameId }, data });
  await audit(userId, `game.${status}`, "Game", game.id);
  logger.info({ gameId, status }, "game status changed");
  return game;
}
