import { randomUUID } from "node:crypto";
import { prisma } from "../db.js";
import { logger } from "../logger.js";
import { audit } from "./audit.js";

type RatingEventType =
  | "player_created"
  | "player_updated"
  | "player_hidden"
  | "player_shown"
  | "game_result"
  | "points_adjustment"
  | "inspiration_adjustment"
  | "correction";

type RawRatingPlayer = {
  id: string;
  displayName: string;
  nickname: string | null;
  avatarUrl: string | null;
  isVisible: boolean;
  gamesPlayed: number;
  totalPoints: number;
  inspirationCount: number;
  averagePointsPerGame: string | number;
  lastGameAt: Date | null;
  lastStatsAt: Date | null;
  updatedAt: Date;
  rank?: number;
};

function makeId(prefix: string) {
  return `${prefix}_${randomUUID().replace(/-/g, "")}`;
}

function toAverage(value: string | number) {
  const numeric = Number(value || 0);
  return Number(numeric.toFixed(2));
}

function ratingDto(player: RawRatingPlayer, rank: number) {
  return {
    rank,
    id: player.id,
    displayName: player.displayName,
    nickname: player.nickname,
    avatarUrl: player.avatarUrl,
    gamesPlayed: Number(player.gamesPlayed || 0),
    totalPoints: Number(player.totalPoints || 0),
    inspirationCount: Number(player.inspirationCount || 0),
    averagePointsPerGame: toAverage(player.averagePointsPerGame),
    lastGameAt: player.lastGameAt?.toISOString() || null,
    lastStatsAt: player.lastStatsAt?.toISOString() || player.updatedAt?.toISOString() || null
  };
}

async function recalculatePlayerStats(db: any, playerId: string) {
  const rows = await db.$queryRawUnsafe(
    `
      SELECT
        COALESCE(SUM("pointsDelta"), 0)::int AS "totalPoints",
        GREATEST(COALESCE(SUM("inspirationDelta"), 0), 0)::int AS "inspirationCount",
        GREATEST(COALESCE(SUM("gamesDelta"), 0), 0)::int AS "gamesPlayed",
        COALESCE(
          MAX(g."gameDate") FILTER (WHERE e."gamesDelta" > 0),
          MAX(e."createdAt") FILTER (WHERE e."gamesDelta" > 0)
        ) AS "lastGameAt"
      FROM "RatingEvent" e
      LEFT JOIN "RatingPlayedGame" g ON g."id" = e."playedGameId"
      WHERE e."playerId" = $1
    `,
    playerId
  ) as Array<{
    totalPoints: number;
    inspirationCount: number;
    gamesPlayed: number;
    lastGameAt: Date | null;
  }>;

  const stats = rows[0] || { totalPoints: 0, inspirationCount: 0, gamesPlayed: 0, lastGameAt: null };
  await db.$executeRawUnsafe(
    `
      UPDATE "RatingPlayer"
      SET
        "totalPoints" = $2,
        "inspirationCount" = $3,
        "gamesPlayed" = $4,
        "lastGameAt" = $5,
        "lastStatsAt" = CURRENT_TIMESTAMP,
        "updatedAt" = CURRENT_TIMESTAMP
      WHERE "id" = $1
    `,
    playerId,
    Number(stats.totalPoints || 0),
    Number(stats.inspirationCount || 0),
    Number(stats.gamesPlayed || 0),
    stats.lastGameAt
  );
}

async function createRatingEvent(db: any, input: {
  playerId: string;
  type: RatingEventType;
  reason: string;
  pointsDelta?: number;
  inspirationDelta?: number;
  gamesDelta?: number;
  playedGameId?: string;
  createdByTelegramId?: bigint;
  createdByMasterId?: string;
  reversalOfEventId?: string;
  idempotencyKey?: string;
}) {
  const id = makeId("revt");
  const rows = await db.$queryRawUnsafe(
    `
      INSERT INTO "RatingEvent" (
        "id", "playerId", "playedGameId", "type", "pointsDelta", "inspirationDelta",
        "gamesDelta", "reason", "createdByTelegramId", "createdByMasterId",
        "reversalOfEventId", "idempotencyKey"
      )
      VALUES ($1, $2, $3, $4::"RatingEventType", $5, $6, $7, $8, $9, $10, $11, $12)
      ON CONFLICT ("idempotencyKey") DO NOTHING
      RETURNING "id"
    `,
    id,
    input.playerId,
    input.playedGameId || null,
    input.type,
    input.pointsDelta || 0,
    input.inspirationDelta || 0,
    input.gamesDelta || 0,
    input.reason,
    input.createdByTelegramId || null,
    input.createdByMasterId || null,
    input.reversalOfEventId || null,
    input.idempotencyKey || null
  ) as Array<{ id: string }>;
  return rows[0]?.id || null;
}

export async function listPublicRating(query: { search?: string; limit?: number; offset?: number } = {}) {
  const search = query.search?.trim().toLowerCase() || null;
  const limit = Math.min(query.limit || 100, 200);
  const offset = query.offset || 0;

  const topRows = await prisma.$queryRawUnsafe<RawRatingPlayer[]>(
    `
      SELECT
        ROW_NUMBER() OVER (
          ORDER BY
            "totalPoints" DESC,
            CASE WHEN "gamesPlayed" > 0 THEN "totalPoints"::numeric / "gamesPlayed" ELSE 0 END DESC,
            "gamesPlayed" DESC,
            LOWER("displayName") ASC,
            "id" ASC
        )::int AS "rank",
        "id", "displayName", "nickname", "avatarUrl", "isVisible", "gamesPlayed",
        "totalPoints", "inspirationCount",
        ROUND(CASE WHEN "gamesPlayed" > 0 THEN "totalPoints"::numeric / "gamesPlayed" ELSE 0 END, 2) AS "averagePointsPerGame",
        "lastGameAt", "lastStatsAt", "updatedAt"
      FROM "RatingPlayer"
      WHERE "isVisible" = true
      ORDER BY "rank" ASC
      LIMIT 3
    `
  );

  const rows = await prisma.$queryRawUnsafe<RawRatingPlayer[]>(
    `
      WITH ranked AS (
        SELECT
          ROW_NUMBER() OVER (
            ORDER BY
              "totalPoints" DESC,
              CASE WHEN "gamesPlayed" > 0 THEN "totalPoints"::numeric / "gamesPlayed" ELSE 0 END DESC,
              "gamesPlayed" DESC,
              LOWER("displayName") ASC,
              "id" ASC
          )::int AS "rank",
          "id", "displayName", "nickname", "avatarUrl", "isVisible", "gamesPlayed",
          "totalPoints", "inspirationCount",
          ROUND(CASE WHEN "gamesPlayed" > 0 THEN "totalPoints"::numeric / "gamesPlayed" ELSE 0 END, 2) AS "averagePointsPerGame",
          "lastGameAt", "lastStatsAt", "updatedAt"
        FROM "RatingPlayer"
        WHERE "isVisible" = true
      )
      SELECT *
      FROM ranked
      WHERE ($1::text IS NULL OR LOWER("displayName") LIKE '%' || $1 || '%' OR LOWER(COALESCE("nickname", '')) LIKE '%' || $1 || '%')
      ORDER BY "rank" ASC
      LIMIT $2 OFFSET $3
    `,
    search,
    limit,
    offset
  );

  const countRows = await prisma.$queryRawUnsafe<Array<{ count: number }>>(
    `
      SELECT COUNT(*)::int AS count
      FROM "RatingPlayer"
      WHERE "isVisible" = true
        AND ($1::text IS NULL OR LOWER("displayName") LIKE '%' || $1 || '%' OR LOWER(COALESCE("nickname", '')) LIKE '%' || $1 || '%')
    `,
    search
  );

  logger.info({ count: rows.length }, "public rating requested");

  return {
    topThree: topRows.map((player) => ratingDto(player, Number(player.rank || 0))),
    players: rows.map((player) => ratingDto(player, Number(player.rank || 0))),
    total: Number(countRows[0]?.count || 0),
    sort: "totalPoints DESC, averagePointsPerGame DESC, gamesPlayed DESC, displayName ASC",
    updatedAt: new Date().toISOString()
  };
}

export async function listRatingPlayersForBot(includeHidden = true) {
  return prisma.$queryRawUnsafe<RawRatingPlayer[]>(
    `
      SELECT
        ROW_NUMBER() OVER (
          ORDER BY
            "totalPoints" DESC,
            CASE WHEN "gamesPlayed" > 0 THEN "totalPoints"::numeric / "gamesPlayed" ELSE 0 END DESC,
            "gamesPlayed" DESC,
            LOWER("displayName") ASC,
            "id" ASC
        )::int AS "rank",
        "id", "displayName", "nickname", "avatarUrl", "isVisible", "gamesPlayed",
        "totalPoints", "inspirationCount",
        ROUND(CASE WHEN "gamesPlayed" > 0 THEN "totalPoints"::numeric / "gamesPlayed" ELSE 0 END, 2) AS "averagePointsPerGame",
        "lastGameAt", "lastStatsAt", "updatedAt"
      FROM "RatingPlayer"
      WHERE ($1::boolean = true OR "isVisible" = true)
      ORDER BY "rank" ASC
      LIMIT 50
    `,
    includeHidden
  );
}

export async function createRatingPlayer(input: {
  displayName: string;
  nickname?: string;
  avatarUrl?: string;
  createdByTelegramId?: bigint;
  createdByMasterId?: string;
  idempotencyKey?: string;
}) {
  const displayName = input.displayName.trim();
  const nickname = input.nickname?.trim() || null;

  const duplicates = await prisma.$queryRawUnsafe<Array<{ id: string }>>(
    `
      SELECT "id"
      FROM "RatingPlayer"
      WHERE LOWER("displayName") = LOWER($1)
        OR ($2::text IS NOT NULL AND LOWER(COALESCE("nickname", '')) = LOWER($2))
      LIMIT 1
    `,
    displayName,
    nickname
  );

  if (duplicates.length) throw new Error("Такой игрок или псевдоним уже есть в рейтинге.");

  const playerId = makeId("rpl");
  await prisma.$transaction(async (tx) => {
    await tx.$executeRawUnsafe(
      `
        INSERT INTO "RatingPlayer" ("id", "displayName", "nickname", "avatarUrl")
        VALUES ($1, $2, $3, $4)
      `,
      playerId,
      displayName,
      nickname,
      input.avatarUrl || null
    );
    await createRatingEvent(tx, {
      playerId,
      type: "player_created",
      reason: "Игрок добавлен в рейтинг через Telegram-бота.",
      createdByTelegramId: input.createdByTelegramId,
      createdByMasterId: input.createdByMasterId,
      idempotencyKey: input.idempotencyKey
    });
  });

  await audit(input.createdByTelegramId?.toString(), "rating.player_created", "RatingPlayer", playerId);
  logger.info({ playerId }, "rating player created");
  return { id: playerId, displayName, nickname };
}

export async function addRatingGameResult(input: {
  playerId: string;
  points: number;
  gameTitle: string;
  gameDate?: Date;
  masterName?: string;
  reason?: string;
  createdByTelegramId?: bigint;
  createdByMasterId?: string;
  idempotencyKey?: string;
}) {
  if (input.points < 0) throw new Error("За сыгранную игру нельзя начислить отрицательные очки.");
  const playedGameId = makeId("rgm");
  const eventId = await prisma.$transaction(async (tx) => {
    await tx.$executeRawUnsafe(
      `
        INSERT INTO "RatingPlayedGame" ("id", "title", "gameDate", "masterName", "notes", "createdByTelegramId")
        VALUES ($1, $2, $3, $4, $5, $6)
      `,
      playedGameId,
      input.gameTitle.trim(),
      input.gameDate || null,
      input.masterName || null,
      input.reason || null,
      input.createdByTelegramId || null
    );
    const id = await createRatingEvent(tx, {
      playerId: input.playerId,
      playedGameId,
      type: "game_result",
      pointsDelta: input.points,
      gamesDelta: 1,
      reason: input.reason || `Сыгранная игра: ${input.gameTitle.trim()}`,
      createdByTelegramId: input.createdByTelegramId,
      createdByMasterId: input.createdByMasterId
    });
    await recalculatePlayerStats(tx, input.playerId);
    return id;
  });

  await audit(input.createdByTelegramId?.toString(), "rating.game_result", "RatingPlayer", input.playerId, { eventId, playedGameId });
  logger.info({ playerId: input.playerId, eventId, points: input.points }, "rating game result added");
  return { eventId, playedGameId };
}

export async function adjustRatingPoints(input: {
  playerId: string;
  pointsDelta: number;
  reason: string;
  createdByTelegramId?: bigint;
  createdByMasterId?: string;
  idempotencyKey?: string;
}) {
  if (!Number.isInteger(input.pointsDelta) || input.pointsDelta === 0) throw new Error("Укажите ненулевое целое изменение очков.");
  const eventId = await prisma.$transaction(async (tx) => {
    const id = await createRatingEvent(tx, {
      playerId: input.playerId,
      type: input.pointsDelta < 0 ? "correction" : "points_adjustment",
      pointsDelta: input.pointsDelta,
      reason: input.reason,
      createdByTelegramId: input.createdByTelegramId,
      createdByMasterId: input.createdByMasterId,
      idempotencyKey: input.idempotencyKey
    });
    await recalculatePlayerStats(tx, input.playerId);
    return id;
  });

  await audit(input.createdByTelegramId?.toString(), "rating.points_adjusted", "RatingPlayer", input.playerId, { eventId, pointsDelta: input.pointsDelta });
  return { eventId };
}

export async function adjustRatingInspiration(input: {
  playerId: string;
  inspirationDelta: number;
  reason: string;
  createdByTelegramId?: bigint;
  createdByMasterId?: string;
  idempotencyKey?: string;
}) {
  if (!Number.isInteger(input.inspirationDelta) || input.inspirationDelta === 0) throw new Error("Укажите ненулевое целое изменение вдохновения.");
  const eventId = await prisma.$transaction(async (tx) => {
    const id = await createRatingEvent(tx, {
      playerId: input.playerId,
      type: input.inspirationDelta < 0 ? "correction" : "inspiration_adjustment",
      inspirationDelta: input.inspirationDelta,
      reason: input.reason,
      createdByTelegramId: input.createdByTelegramId,
      createdByMasterId: input.createdByMasterId,
      idempotencyKey: input.idempotencyKey
    });
    await recalculatePlayerStats(tx, input.playerId);
    return id;
  });

  await audit(input.createdByTelegramId?.toString(), "rating.inspiration_adjusted", "RatingPlayer", input.playerId, {
    eventId,
    inspirationDelta: input.inspirationDelta
  });
  return { eventId };
}

export async function setRatingPlayerVisibility(input: {
  playerId: string;
  isVisible: boolean;
  reason: string;
  createdByTelegramId?: bigint;
  createdByMasterId?: string;
}) {
  const eventId = await prisma.$transaction(async (tx) => {
    await tx.$executeRawUnsafe(
      `UPDATE "RatingPlayer" SET "isVisible" = $2, "updatedAt" = CURRENT_TIMESTAMP WHERE "id" = $1`,
      input.playerId,
      input.isVisible
    );
    return createRatingEvent(tx, {
      playerId: input.playerId,
      type: input.isVisible ? "player_shown" : "player_hidden",
      reason: input.reason,
      createdByTelegramId: input.createdByTelegramId,
      createdByMasterId: input.createdByMasterId
    });
  });

  await audit(input.createdByTelegramId?.toString(), input.isVisible ? "rating.player_shown" : "rating.player_hidden", "RatingPlayer", input.playerId, { eventId });
  return { eventId };
}

export async function listRatingHistoryForBot(limit = 10) {
  return prisma.$queryRawUnsafe<Array<{
    id: string;
    displayName: string;
    type: RatingEventType;
    pointsDelta: number;
    inspirationDelta: number;
    gamesDelta: number;
    reason: string;
    createdAt: Date;
  }>>(
    `
      SELECT e."id", p."displayName", e."type", e."pointsDelta", e."inspirationDelta", e."gamesDelta", e."reason", e."createdAt"
      FROM "RatingEvent" e
      JOIN "RatingPlayer" p ON p."id" = e."playerId"
      ORDER BY e."createdAt" DESC
      LIMIT $1
    `,
    Math.min(limit, 30)
  );
}
