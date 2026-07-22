import cors from "cors";
import express from "express";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { z } from "zod";
import { getBotHealth } from "../bot/status.js";
import { config, siteOrigins } from "../config.js";
import { prisma } from "../db.js";
import { logger } from "../logger.js";
import { ConsentRequiredError, CONSENT_REQUIRED_CODE } from "../services/consent.js";
import { archivePastGames, createGameSignup, getPublicGame, listPublicGames, setGameStatus } from "../services/games.js";
import { withdrawConsent } from "../services/privacy.js";
import { listPublicRating } from "../services/rating.js";
import { createServiceRequest } from "../services/serviceRequests.js";
import { validateDateTime } from "../utils/validation.js";
import { rateLimit } from "./rateLimit.js";

const root = dirname(dirname(dirname(fileURLToPath(import.meta.url))));

const publicQuerySchema = z.object({
  dateFrom: z.coerce.date().optional(),
  dateTo: z.coerce.date().optional(),
  masterId: z.string().optional(),
  system: z.string().optional(),
  limit: z.coerce.number().int().positive().max(100).optional(),
  offset: z.coerce.number().int().min(0).optional()
});

const publicRatingQuerySchema = z.object({
  search: z.string().trim().max(80).optional(),
  limit: z.coerce.number().int().positive().max(200).optional(),
  offset: z.coerce.number().int().min(0).optional()
});

const internalGameSchema = z.object({
  masterId: z.string(),
  title: z.string().trim().min(3).max(100),
  description: z.string().trim().min(20).max(1000),
  gameSystem: z.string().trim().min(2).max(80),
  experienceLevel: z.string().trim().min(2).max(80),
  ageRating: z.string().trim().min(2).max(30),
  date: z.string(),
  time: z.string(),
  durationMinutes: z.number().int().positive().max(12 * 60).optional(),
  minPlayers: z.number().int().min(1).max(20),
  maxPlayers: z.number().int().min(1).max(20),
  price: z.string(),
  currency: z.string().default("BYN"),
  imageUrl: z.string().url().optional(),
  contactUrl: z.string().url()
});

const consentSchema = z.object({
  consentGiven: z.boolean().optional(),
  consentVersion: z.string().trim().optional(),
  privacyPolicyVersion: z.string().trim().optional()
});

const serviceRequestSchema = z.object({
  name: z.string().trim().min(2).max(80),
  contact: z.string().trim().min(3).max(120),
  service: z.string().trim().min(2).max(120),
  serviceType: z.string().trim().max(80).optional().or(z.literal("")),
  desiredDate: z.string().trim().max(80).optional().or(z.literal("")),
  participants: z.coerce.number().int().positive().max(200).optional().or(z.literal("")),
  city: z.string().trim().max(80).optional().or(z.literal("")),
  comment: z.string().trim().max(1500).optional().or(z.literal(""))
}).merge(consentSchema);

const gameSignupSchema = z.object({
  gameId: z.string().trim().min(1),
  playerName: z.string().trim().min(2).max(80),
  contact: z.string().trim().min(3).max(120),
  seats: z.coerce.number().int().positive().max(20).default(1),
  comment: z.string().trim().max(1000).optional().or(z.literal(""))
}).merge(consentSchema);

const withdrawConsentSchema = z.object({
  entityType: z.enum(["GameSignup", "ServiceRequest", "ContactRequest"]),
  requestId: z.string().trim().min(1),
  anonymize: z.boolean().optional()
});

function errorDetails(error: unknown) {
  if (error instanceof Error) return { name: error.name, message: error.message };
  return { message: String(error) };
}

function publicErrorMessage(error: unknown) {
  if (error instanceof ConsentRequiredError) return error.message;
  if (error instanceof z.ZodError) return "Проверьте данные.";
  if (error instanceof Error && !error.name.startsWith("Prisma")) return error.message;
  return "Сервер временно недоступен. Попробуйте позже.";
}

export function createApp() {
  const app = express();

  app.use(express.json({ limit: "1mb" }));
  app.use("/api", rateLimit(180, 60_000));
  app.use(
    cors({
      origin(origin, callback) {
        if (!origin || origin === "null" || siteOrigins.includes(origin)) return callback(null, true);
        return callback(new Error("CORS origin is not allowed"));
      }
    })
  );

  app.get("/health", async (_request, response) => {
    const started = Date.now();
    try {
      await prisma.$queryRaw`SELECT 1`;
      response.json({
        ok: true,
        backend: "ok",
        database: "ok",
        bot: getBotHealth(),
        checkedAt: new Date().toISOString(),
        latencyMs: Date.now() - started
      });
    } catch (error) {
      logger.error({ error: errorDetails(error) }, "health check failed");
      response.status(503).json({
        ok: false,
        backend: "ok",
        database: "error",
        bot: getBotHealth(),
        checkedAt: new Date().toISOString()
      });
    }
  });

  app.get("/api/games", async (request, response, next) => {
    try {
      const query = publicQuerySchema.parse(request.query);
      response.json({ games: await listPublicGames(query) });
    } catch (error) {
      next(error);
    }
  });

  app.get("/api/games/:id", async (request, response, next) => {
    try {
      const game = await getPublicGame(request.params.id);
      if (!game) return response.status(404).json({ error: "Игра не найдена." });
      response.json({ game });
    } catch (error) {
      next(error);
    }
  });

  app.get("/api/rating", async (request, response, next) => {
    try {
      const query = publicRatingQuerySchema.parse(request.query);
      response.json(await listPublicRating(query));
    } catch (error) {
      next(error);
    }
  });

  app.post("/api/game-signups", async (request, response, next) => {
    try {
      const body = gameSignupSchema.parse(request.body);
      const result = await createGameSignup({
        gameId: body.gameId,
        playerName: body.playerName,
        contact: body.contact,
        seats: body.seats,
        comment: body.comment || undefined,
        consent: {
          consentGiven: body.consentGiven,
          consentVersion: body.consentVersion,
          privacyPolicyVersion: body.privacyPolicyVersion
        }
      });
      response.status(201).json({
        ok: true,
        message: "Запись сохранена. Мастер получил уведомление.",
        signupId: result.signup.id,
        game: result.game
      });
    } catch (error) {
      next(error);
    }
  });

  app.post("/api/service-requests", async (request, response, next) => {
    try {
      const body = serviceRequestSchema.parse(request.body);
      const serviceRequest = await createServiceRequest({
        name: body.name,
        contact: body.contact,
        service: body.service,
        serviceType: body.serviceType || undefined,
        desiredDate: body.desiredDate || undefined,
        participants: typeof body.participants === "number" ? body.participants : undefined,
        city: body.city || undefined,
        comment: body.comment || undefined,
        consent: {
          consentGiven: body.consentGiven,
          consentVersion: body.consentVersion,
          privacyPolicyVersion: body.privacyPolicyVersion
        }
      });
      response.status(201).json({
        ok: true,
        message: "Заявка сохранена. Мы свяжемся с вами.",
        requestId: serviceRequest.id
      });
    } catch (error) {
      next(error);
    }
  });

  app.use("/api/internal", (request, response, next) => {
    const token = request.header("x-internal-token");
    if (token !== config.INTERNAL_API_TOKEN) {
      response.status(401).json({ error: "Нужна авторизация." });
      return;
    }
    next();
  });

  app.post("/api/internal/archive-past-games", async (_request, response, next) => {
    try {
      response.json({ archived: await archivePastGames() });
    } catch (error) {
      next(error);
    }
  });

  app.post("/api/internal/games", async (request, response, next) => {
    try {
      const body = internalGameSchema.parse(request.body);
      if (body.minPlayers > body.maxPlayers) {
        return response.status(422).json({ error: "Минимум игроков не может быть больше максимума." });
      }

      const dateTime = validateDateTime(body.date, body.time);
      if (!dateTime.ok) return response.status(422).json({ error: dateTime.message });

      const start = dateTime.value.toUTC().toJSDate();
      const end = body.durationMinutes ? dateTime.value.plus({ minutes: body.durationMinutes }).toUTC().toJSDate() : null;

      const game = await prisma.game.create({
        data: {
          masterId: body.masterId,
          title: body.title,
          description: body.description,
          gameSystem: body.gameSystem,
          experienceLevel: body.experienceLevel,
          ageRating: body.ageRating,
          dateTimeStart: start,
          dateTimeEnd: end,
          durationMinutes: body.durationMinutes,
          minPlayers: body.minPlayers,
          maxPlayers: body.maxPlayers,
          price: body.price,
          currency: body.currency,
          imageUrl: body.imageUrl,
          contactUrl: body.contactUrl,
          status: config.AUTO_PUBLISH ? "published" : "pending",
          publishedAt: config.AUTO_PUBLISH ? new Date() : null
        }
      });

      logger.info({ gameId: game.id, status: game.status }, "internal game created");
      response.status(201).json({ game });
    } catch (error) {
      next(error);
    }
  });

  app.patch("/api/internal/games/:id/status", async (request, response, next) => {
    try {
      const body = z.object({ status: z.enum(["draft", "pending", "published", "completed", "cancelled", "archived"]) }).parse(request.body);
      response.json({ game: await setGameStatus(request.params.id, body.status) });
    } catch (error) {
      next(error);
    }
  });

  app.post("/api/internal/privacy/withdraw-consent", async (request, response, next) => {
    try {
      const body = withdrawConsentSchema.parse(request.body);
      response.json(await withdrawConsent(body));
    } catch (error) {
      next(error);
    }
  });

  app.use("/uploads", express.static(join(root, config.FILE_STORAGE_DIR)));
  app.use(express.static(root));
  app.get("/rating", (_request, response) => response.sendFile(join(root, "rating.html")));
  app.get("*", (_request, response) => response.sendFile(join(root, "index.html")));

  app.use((error: unknown, _request: express.Request, response: express.Response, _next: express.NextFunction) => {
    logger.error({ error: errorDetails(error) }, "request failed");
    if (error instanceof ConsentRequiredError) {
      response.status(422).json({ code: CONSENT_REQUIRED_CODE, error: publicErrorMessage(error) });
      return;
    }
    if (error instanceof z.ZodError) {
      response.status(422).json({ error: publicErrorMessage(error), details: error.flatten() });
      return;
    }
    const status = error instanceof Error && !error.name.startsWith("Prisma") ? 422 : 500;
    response.status(status).json({ error: publicErrorMessage(error) });
  });

  return app;
}

export function startApi() {
  const app = createApp();
  return app.listen(config.PORT, () => {
    logger.info(`Taverna API is running at ${config.SITE_BASE_URL}`);
  });
}
