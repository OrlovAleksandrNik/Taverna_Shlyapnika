import { Bot, Context, InlineKeyboard } from "grammy";
import { run } from "@grammyjs/runner";
import { DateTime } from "luxon";
import { config, adminTelegramIds } from "../config.js";
import { prisma } from "../db.js";
import { logger } from "../logger.js";
import { audit } from "../services/audit.js";
import {
  createGalleryPost,
  createGalleryPublicId,
  listGalleryPostsForBot,
  setGalleryPostStatus,
  storeTelegramGalleryImage
} from "../services/gallery.js";
import {
  addRatingGameResult,
  adjustRatingInspiration,
  adjustRatingPoints,
  createRatingPlayer,
  listRatingHistoryForBot,
  listRatingPlayersForBot,
  setRatingPlayerVisibility
} from "../services/rating.js";
import { formatForTelegram, nowInTavernZone, toUtcDate } from "../utils/time.js";
import { cleanText, gameText, parsePlayers, parsePrice, telegramContactSchema, validateDateTime } from "../utils/validation.js";
import { markBotDisabled, markBotError, markBotRunning, markBotStarting, markBotUpdate } from "./status.js";

type Draft = {
  displayName?: string;
  contactUrl?: string;
  title?: string;
  date?: string;
  time?: string;
  durationMinutes?: number;
  description?: string;
  minPlayers?: number;
  maxPlayers?: number;
  price?: string;
  currency?: string;
  gameSystem?: string;
  experienceLevel?: string;
  ageRating?: string;
  contactOverride?: string;
  ratingDisplayName?: string;
  galleryType?: "photo" | "story";
  galleryTitle?: string;
  galleryDescription?: string;
  galleryStoryContent?: string;
  galleryCategory?: "games" | "events" | "heroes" | "tavern" | "miniatures" | "other";
  galleryEventDate?: string;
  galleryMedia?: Array<{ fileId: string; source: "photo" | "document" }>;
};

type Session = {
  telegramUserId: bigint;
  state: string;
  draft: Draft;
};

function userId(ctx: Context) {
  return ctx.from?.id ? BigInt(ctx.from.id) : null;
}

function username(ctx: Context) {
  return ctx.from?.username ? `@${ctx.from.username}` : null;
}

function redactSecrets(value?: string) {
  if (!value) return value;
  return value
    .replace(/bot\d+:[A-Za-z0-9_-]+/g, "bot[redacted]")
    .replace(/\d{8,12}:[A-Za-z0-9_-]{30,}/g, "[redacted-token]");
}

function botErrorDetails(error: unknown) {
  const botError = error as {
    name?: string;
    message?: string;
    error?: { name?: string; message?: string; method?: string; error_code?: number; description?: string };
  };

  return {
    name: botError.name,
    message: redactSecrets(botError.message),
    cause: botError.error
      ? {
          name: botError.error.name,
          message: redactSecrets(botError.error.message),
          method: botError.error.method,
          errorCode: botError.error.error_code,
          description: redactSecrets(botError.error.description)
        }
      : undefined
  };
}

function mainMenu(isAdmin = false) {
  const keyboard = new InlineKeyboard()
    .text("🎲 Афиша и игры", "games_menu")
    .text("🖼 Галерея", "gallery_menu").row();

  if (isAdmin) keyboard.text("🏆 Рейтинг", "rating_menu").text("⚙️ Администрирование", "admin").row();
  return keyboard.text("👤 Мой профиль", "profile").text("Помощь", "help");
}

function gamesMenuKeyboard() {
  return new InlineKeyboard()
    .text("Создать заявку на игру", "create_game").row()
    .text("Мои игры", "my_games")
    .text("Предстоящие игры", "upcoming_games").row()
    .text("Прошедшие игры", "past_games").row()
    .text("⬅️ Назад", "menu")
    .text("❌ Отмена", "cancel");
}

function startMenu() {
  return new InlineKeyboard()
    .text("Зарегистрироваться как мастер", "register")
    .row()
    .text("Войти", "login")
    .text("Помощь", "help");
}

function navKeyboard() {
  return new InlineKeyboard()
    .text("Назад", "back")
    .text("Отмена", "cancel")
    .row()
    .text("Главное меню", "menu");
}

function contactChoiceKeyboard() {
  return new InlineKeyboard()
    .text("Использовать контакт профиля", "use_profile_contact")
    .row()
    .text("Указать другой", "manual_game_contact");
}

async function getSession(id: bigint): Promise<Session> {
  const session = await prisma.botSession.upsert({
    where: { telegramUserId: id },
    create: { telegramUserId: id, state: "idle", draft: {} },
    update: {}
  });

  return {
    telegramUserId: session.telegramUserId,
    state: session.state,
    draft: (session.draft || {}) as Draft
  };
}

async function saveSession(id: bigint, state: string, draft: Draft = {}) {
  await prisma.botSession.upsert({
    where: { telegramUserId: id },
    create: { telegramUserId: id, state, draft },
    update: { state, draft }
  });
}

async function resetSession(id: bigint) {
  await saveSession(id, "idle", {});
}

async function findMaster(id: bigint) {
  return prisma.master.findUnique({ where: { telegramUserId: id } });
}

async function requireActiveMaster(ctx: Context) {
  const id = userId(ctx);
  if (!id) return null;

  const master = await findMaster(id);
  if (!master) {
    await ctx.reply("Сначала нужно зарегистрироваться как мастер.", { reply_markup: startMenu() });
    return null;
  }

  if (master.status === "blocked") {
    await ctx.reply("Ваш профиль заблокирован администратором. Публикация игр недоступна.");
    return null;
  }

  return master;
}

function registrationSummary(draft: Draft) {
  return [
    "Проверьте данные:",
    "",
    `Имя мастера: ${draft.displayName}`,
    `Telegram: ${draft.contactUrl}`,
    `Дата регистрации: ${DateTime.now().setZone(config.TAVERN_TIMEZONE).toFormat("dd.LL.yyyy")}`
  ].join("\n");
}

function gamePreview(draft: Draft) {
  return [
    "Проверьте карточку игры:",
    "",
    `Название: ${draft.title}`,
    `Дата и время: ${draft.date} ${draft.time}`,
    `Продолжительность: ${draft.durationMinutes ? `${draft.durationMinutes / 60} ч.` : "не указана"}`,
    `Описание: ${draft.description}`,
    `Система: ${draft.gameSystem}`,
    `Уровень: ${draft.experienceLevel}`,
    `Возраст: ${draft.ageRating}`,
    `Игроки: ${draft.minPlayers}-${draft.maxPlayers}`,
    `Стоимость: ${draft.price} ${draft.currency}`,
    `Контакт: ${draft.contactOverride || draft.contactUrl}`,
  ].join("\n");
}

function dateKeyboard() {
  const keyboard = new InlineKeyboard();
  const today = nowInTavernZone().startOf("day");
  for (let index = 0; index < 7; index += 1) {
    const day = today.plus({ days: index });
    keyboard.text(day.setLocale("ru").toFormat("dd LLL"), `date:${day.toISODate()}`);
    if (index % 3 === 2) keyboard.row();
  }
  return keyboard.row().text("Ввести дату вручную", "date_manual").row().text("Отмена", "cancel");
}

function durationKeyboard() {
  return new InlineKeyboard()
    .text("2 часа", "duration:120")
    .text("3 часа", "duration:180").row()
    .text("4 часа", "duration:240")
    .text("5 часов", "duration:300").row()
    .text("Пропустить", "duration:0")
    .text("Другое", "duration_manual");
}

function optionKeyboard(prefix: string, values: string[]) {
  const keyboard = new InlineKeyboard();
  values.forEach((value, index) => {
    keyboard.text(value, `${prefix}:${value}`);
    if (index % 2 === 1) keyboard.row();
  });
  return keyboard.row().text("Ввести свой вариант", `${prefix}:manual`).row().text("Отмена", "cancel");
}

async function sendStart(ctx: Context) {
  const id = userId(ctx);
  const master = id ? await findMaster(id) : null;

  if (master) {
    await ctx.reply(`Добро пожаловать обратно, ${master.displayName}.`, {
      reply_markup: mainMenu(adminTelegramIds.has(String(ctx.from?.id)) || master.role === "admin")
    });
    return;
  }

  await ctx.reply(
    "Добро пожаловать в систему мастеров “Таверны Шляпника”.\n\nЗдесь вы можете зарегистрироваться как мастер, создавать заявки на игры и публиковать их в афише таверны.",
    { reply_markup: startMenu() }
  );
}

async function startRegistration(ctx: Context) {
  const id = userId(ctx);
  if (!id) return;
  await saveSession(id, "register:name", {});
  await ctx.reply("Как вас представить игрокам? Напишите имя или мастерский псевдоним.", { reply_markup: navKeyboard() });
}

async function askContact(ctx: Context, draft: Draft) {
  const id = userId(ctx);
  if (!id) return;
  await saveSession(id, "register:contact", draft);

  const keyboard = new InlineKeyboard();
  const current = username(ctx);
  if (current) keyboard.text(`Использовать ${current}`, "use_current_username").row();
  keyboard.text("Ввести ссылку вручную", "manual_contact").row().text("Отмена", "cancel");

  const noUsernameHint = current
    ? ""
    : "\n\nУ вас не указан Telegram username. Его можно добавить в Telegram: Настройки → Имя пользователя. Либо укажите другой контакт в формате @username или https://t.me/username.";

  await ctx.reply(`Укажите Telegram-контакт для связи с игроками.${noUsernameHint}`, { reply_markup: keyboard });
}

async function confirmRegistration(ctx: Context, draft: Draft) {
  const id = userId(ctx);
  if (!id) return;
  await saveSession(id, "register:confirm", draft);
  await ctx.reply(registrationSummary(draft), {
    reply_markup: new InlineKeyboard()
      .text("Подтвердить", "confirm_registration").row()
      .text("Изменить имя", "register_edit_name")
      .text("Изменить контакт", "register_edit_contact").row()
      .text("Отмена", "cancel")
  });
}

async function createMaster(ctx: Context, draft: Draft) {
  const id = userId(ctx);
  if (!id || !draft.displayName || !draft.contactUrl) return;

  const isAdmin = adminTelegramIds.has(String(ctx.from?.id));
  const master = await prisma.master.upsert({
    where: { telegramUserId: id },
    create: {
      telegramUserId: id,
      telegramUsername: ctx.from?.username || null,
      displayName: draft.displayName,
      contactUrl: draft.contactUrl,
      role: isAdmin ? "admin" : "master"
    },
    update: {
      telegramUsername: ctx.from?.username || null,
      displayName: draft.displayName,
      contactUrl: draft.contactUrl,
      role: isAdmin ? "admin" : undefined
    }
  });

  await audit(String(id), "master.registered", "Master", master.id);
  await resetSession(id);
  await ctx.reply("Готово. Регистрация завершена, теперь можно создавать игры.", {
    reply_markup: mainMenu(isAdmin || master.role === "admin")
  });
}

async function beginGameDraft(ctx: Context) {
  const id = userId(ctx);
  const master = await requireActiveMaster(ctx);
  if (!id || !master) return;

  await saveSession(id, "create:title", { contactUrl: master.contactUrl });
  await ctx.reply("Напишите название игры.", { reply_markup: navKeyboard() });
}

async function listGames(ctx: Context, mode: "all" | "upcoming" | "past") {
  const master = await requireActiveMaster(ctx);
  if (!master) return;

  const now = new Date();
  const where =
    mode === "upcoming"
      ? { masterId: master.id, dateTimeStart: { gte: now }, status: { in: ["published", "pending"] } }
      : mode === "past"
        ? { masterId: master.id, OR: [{ dateTimeStart: { lt: now } }, { status: { in: ["completed", "cancelled", "archived"] } }] }
        : { masterId: master.id };

  const games = await prisma.game.findMany({
    where: where as any,
    orderBy: { dateTimeStart: mode === "past" ? "desc" : "asc" },
    take: 10
  });

  if (!games.length) {
    await ctx.reply("Пока здесь нет игр.", { reply_markup: mainMenu(master.role === "admin") });
    return;
  }

  for (const game of games) {
    const keyboard = new InlineKeyboard();
    if (!["completed", "cancelled", "archived"].includes(game.status)) {
      keyboard.text("Редактировать", `edit_game:${game.id}`).text("Отменить", `cancel_game:${game.id}`);
    }

    await ctx.reply(
      [
        `${game.title}`,
        `${formatForTelegram(game.dateTimeStart)}`,
        `Статус: ${game.status}`,
        `Игроки: ${game.minPlayers}-${game.maxPlayers}`,
        `Стоимость: ${game.price} ${game.currency}`
      ].join("\n"),
      { reply_markup: keyboard.inline_keyboard.length ? keyboard : undefined }
    );
  }
}

async function showProfile(ctx: Context) {
  const master = await requireActiveMaster(ctx);
  if (!master) return;

  const [created, completed] = await Promise.all([
    prisma.game.count({ where: { masterId: master.id } }),
    prisma.game.count({ where: { masterId: master.id, status: "completed" } })
  ]);

  await ctx.reply(
    [
      "Мой профиль",
      "",
      `Имя: ${master.displayName}`,
      `Telegram: ${master.contactUrl}`,
      `Дата регистрации: ${formatForTelegram(master.createdAt)}`,
      `Создано игр: ${created}`,
      `Завершено игр: ${completed}`
    ].join("\n"),
    {
      reply_markup: new InlineKeyboard()
        .text("Изменить имя", "profile_edit_name")
        .text("Изменить Telegram", "profile_edit_contact").row()
        .text("Главное меню", "menu")
    }
  );
}

function isRatingAdmin(ctx: Context, master: { role: string } | null | undefined) {
  return Boolean(master && (master.role === "admin" || adminTelegramIds.has(String(ctx.from?.id))));
}

function isContentAdmin(ctx: Context, master: { role: string } | null | undefined) {
  return Boolean(master && (master.role === "admin" || adminTelegramIds.has(String(ctx.from?.id))));
}

async function requireRatingAdmin(ctx: Context) {
  const id = userId(ctx);
  if (!id) return null;
  const master = await findMaster(id);

  if (!master || master.status !== "active" || !isRatingAdmin(ctx, master)) {
    await ctx.reply("Управление рейтингом доступно только администраторам Таверны.");
    return null;
  }

  return master;
}

function galleryMenuKeyboard() {
  return new InlineKeyboard()
    .text("➕ Добавить фотографию", "gallery_add_photo").row()
    .text("📚 Добавить историю", "gallery_add_story").row()
    .text("📋 Список публикаций", "gallery_posts").row()
    .text("⬅️ Назад", "menu")
    .text("❌ Отмена", "cancel");
}

function galleryMediaKeyboard() {
  return new InlineKeyboard()
    .text("Далее", "gallery_media_done").row()
    .text("❌ Отмена", "cancel");
}

function galleryCategoryKeyboard() {
  return new InlineKeyboard()
    .text("Игры", "gallery_category:games")
    .text("События", "gallery_category:events").row()
    .text("Герои", "gallery_category:heroes")
    .text("Таверна", "gallery_category:tavern").row()
    .text("Миниатюры", "gallery_category:miniatures")
    .text("Другое", "gallery_category:other").row()
    .text("❌ Отмена", "cancel");
}

function galleryPublishKeyboard() {
  return new InlineKeyboard()
    .text("Опубликовать", "gallery_publish")
    .text("Сохранить черновик", "gallery_draft").row()
    .text("❌ Отмена", "cancel");
}

function galleryStoryMediaChoiceKeyboard() {
  return new InlineKeyboard()
    .text("Добавить фото", "gallery_story_add_media")
    .text("Без фото", "gallery_story_skip_media").row()
    .text("❌ Отмена", "cancel");
}

function galleryConfirmKeyboard() {
  return new InlineKeyboard()
    .text("Подтвердить", "gallery_confirm").row()
    .text("❌ Отмена", "cancel");
}

async function showGalleryMenu(ctx: Context) {
  const master = await requireActiveMaster(ctx);
  if (!master) return;
  const isAdmin = isContentAdmin(ctx, master);
  const count = await prisma.galleryPost.count({ where: isAdmin ? {} : { authorMasterId: master.id } });
  await ctx.reply(
    [
      "Галерея",
      "",
      `Публикаций в управлении: ${count}`,
      "Здесь можно добавить фотографии и истории. На сайте отображаются только опубликованные записи."
    ].join("\n"),
    { reply_markup: galleryMenuKeyboard() }
  );
}

async function startGalleryPhoto(ctx: Context) {
  const id = userId(ctx);
  const master = await requireActiveMaster(ctx);
  if (!id || !master) return;
  await saveSession(id, "gallery:photo:media", { galleryType: "photo", galleryMedia: [] });
  await ctx.reply("Отправьте одну или несколько фотографий JPEG, PNG или WEBP. После загрузки нажмите «Далее».", {
    reply_markup: galleryMediaKeyboard()
  });
}

async function startGalleryStory(ctx: Context) {
  const id = userId(ctx);
  const master = await requireActiveMaster(ctx);
  if (!id || !master) return;
  await saveSession(id, "gallery:story:title", { galleryType: "story", galleryMedia: [] });
  await ctx.reply("Введите заголовок истории для галереи.", { reply_markup: navKeyboard() });
}

function parseOptionalGalleryDate(value?: string) {
  const text = (value || "").trim();
  if (!text || /^нет$/i.test(text) || /^пропустить$/i.test(text)) return undefined;
  const parsed = DateTime.fromISO(text, { zone: config.TAVERN_TIMEZONE });
  if (!parsed.isValid) throw new Error("Введите дату события в формате YYYY-MM-DD или напишите «нет».");
  return text;
}

function galleryDraftPreview(draft: Draft, status: "published" | "draft") {
  return [
    status === "published" ? "Публикация будет видна на сайте:" : "Публикация будет сохранена как черновик:",
    "",
    `Тип: ${draft.galleryType === "story" ? "история" : "фотографии"}`,
    `Название: ${draft.galleryTitle}`,
    `Категория: ${draft.galleryCategory}`,
    draft.galleryDescription ? `Описание: ${draft.galleryDescription}` : "",
    draft.galleryStoryContent ? `История: ${draft.galleryStoryContent.slice(0, 700)}${draft.galleryStoryContent.length > 700 ? "..." : ""}` : "",
    draft.galleryEventDate ? `Дата события: ${draft.galleryEventDate}` : "",
    `Фотографий: ${draft.galleryMedia?.length || 0}`
  ].filter(Boolean).join("\n");
}

async function createGalleryFromDraft(ctx: Context, draft: Draft, status: "published" | "draft") {
  const id = userId(ctx);
  const master = await requireActiveMaster(ctx);
  if (!id || !master || !draft.galleryTitle || !draft.galleryCategory || !draft.galleryType) return;

  if (draft.galleryType === "photo" && !draft.galleryMedia?.length) {
    await ctx.reply("Для фотопубликации нужна хотя бы одна фотография.");
    return;
  }

  const publicId = createGalleryPublicId();
  const media = [];
  for (const [index, item] of (draft.galleryMedia || []).entries()) {
    media.push(await storeTelegramGalleryImage({
      api: ctx.api,
      fileId: item.fileId,
      postPublicId: publicId,
      altText: draft.galleryTitle,
      sortOrder: index
    }));
  }

  const post = await createGalleryPost({
    publicId,
    type: draft.galleryType,
    title: draft.galleryTitle,
    description: draft.galleryDescription,
    storyContent: draft.galleryStoryContent,
    category: draft.galleryCategory,
    eventDate: draft.galleryEventDate ? toUtcDate(DateTime.fromISO(draft.galleryEventDate, { zone: config.TAVERN_TIMEZONE }).startOf("day")) : undefined,
    authorMasterId: master.id,
    status,
    media,
    createdByTelegramId: id
  });

  await resetSession(id);
  await ctx.reply(
    status === "published"
      ? `Публикация добавлена в галерею: ${post.title}`
      : `Черновик сохранён: ${post.title}`,
    { reply_markup: galleryMenuKeyboard() }
  );
}

async function listGalleryPosts(ctx: Context) {
  const id = userId(ctx);
  const master = await requireActiveMaster(ctx);
  if (!id || !master) return;
  const isAdmin = isContentAdmin(ctx, master);
  const posts = await listGalleryPostsForBot(master.id, isAdmin);
  if (!posts.length) {
    await ctx.reply("Публикаций пока нет.", { reply_markup: galleryMenuKeyboard() });
    return;
  }

  for (const post of posts) {
    const keyboard = new InlineKeyboard();
    if (post.status !== "published") keyboard.text("Опубликовать", `gallery_set_published:${post.id}`).row();
    if (post.status !== "hidden") keyboard.text("Скрыть", `gallery_set_hidden:${post.id}`).row();
    if (post.status === "hidden") keyboard.text("Вернуть", `gallery_set_published:${post.id}`).row();
    keyboard.text("Назад", "gallery_menu");
    await ctx.reply(
      [
        post.title,
        `Статус: ${post.status}`,
        `Категория: ${post.category}`,
        `Фотографий: ${post.media.length}`
      ].join("\n"),
      { reply_markup: keyboard }
    );
  }
}

function ratingMenuKeyboard() {
  return new InlineKeyboard()
    .text("Добавить игрока", "rating_create_player").row()
    .text("Сыгранная игра", "rating_add_game")
    .text("Очки", "rating_adjust_points").row()
    .text("Вдохновение", "rating_adjust_inspiration")
    .text("Скрыть/вернуть", "rating_visibility").row()
    .text("Список игроков", "rating_players")
    .text("История", "rating_history").row()
    .text("Главное меню", "menu");
}

async function showRatingMenu(ctx: Context) {
  const master = await requireRatingAdmin(ctx);
  if (!master) return;
  const players = await listRatingPlayersForBot(true);
  await ctx.reply(
    [
      "Рейтинг игроков",
      "",
      `Игроков в базе: ${players.length}`,
      "Все изменения выполняются здесь, через Telegram-бота. На сайте рейтинг только отображается."
    ].join("\n"),
    { reply_markup: ratingMenuKeyboard() }
  );
}

function ratingPlayerLine(player: {
  rank?: number;
  displayName: string;
  nickname: string | null;
  isVisible: boolean;
  gamesPlayed: number;
  totalPoints: number;
  inspirationCount: number;
  averagePointsPerGame: string | number;
}) {
  const hidden = player.isVisible ? "" : " · скрыт";
  const nickname = player.nickname ? ` (${player.nickname})` : "";
  return `#${player.rank || "-"} ${player.displayName}${nickname}: ${player.totalPoints} очк., игр ${player.gamesPlayed}, ср. ${Number(player.averagePointsPerGame || 0).toFixed(2)}, вдохн. ${player.inspirationCount}${hidden}`;
}

async function showRatingPlayers(ctx: Context) {
  const master = await requireRatingAdmin(ctx);
  if (!master) return;
  const players = await listRatingPlayersForBot(true);
  if (!players.length) {
    await ctx.reply("В рейтинге пока нет игроков.", { reply_markup: ratingMenuKeyboard() });
    return;
  }

  await ctx.reply(["Игроки рейтинга:", "", ...players.map(ratingPlayerLine)].join("\n"), { reply_markup: ratingMenuKeyboard() });
}

async function showRatingHistory(ctx: Context) {
  const master = await requireRatingAdmin(ctx);
  if (!master) return;
  const events = await listRatingHistoryForBot(10);
  if (!events.length) {
    await ctx.reply("История рейтинга пока пустая.", { reply_markup: ratingMenuKeyboard() });
    return;
  }

  await ctx.reply(
    [
      "Последние операции рейтинга:",
      "",
      ...events.map((event) => {
        const deltas = [
          event.pointsDelta ? `${event.pointsDelta > 0 ? "+" : ""}${event.pointsDelta} очк.` : "",
          event.gamesDelta ? `${event.gamesDelta > 0 ? "+" : ""}${event.gamesDelta} игр.` : "",
          event.inspirationDelta ? `${event.inspirationDelta > 0 ? "+" : ""}${event.inspirationDelta} вдохн.` : ""
        ].filter(Boolean).join(", ");
        return `${formatForTelegram(event.createdAt)} · ${event.displayName} · ${deltas || event.type} · ${event.reason}`;
      })
    ].join("\n"),
    { reply_markup: ratingMenuKeyboard() }
  );
}

async function selectRatingPlayer(ctx: Context, action: "game" | "points" | "inspiration" | "visibility") {
  const master = await requireRatingAdmin(ctx);
  if (!master) return;
  const players = await listRatingPlayersForBot(action === "visibility");
  if (!players.length) {
    await ctx.reply("Сначала добавьте игрока в рейтинг.", { reply_markup: ratingMenuKeyboard() });
    return;
  }

  const keyboard = new InlineKeyboard();
  players.slice(0, 20).forEach((player) => {
    const prefix = action === "game" ? "rating_game" : action === "points" ? "rating_points" : action === "inspiration" ? "rating_insp" : "rating_vis";
    keyboard.text(`${player.rank || "-"} · ${player.displayName}${player.isVisible ? "" : " · скрыт"}`, `${prefix}:${player.id}`).row();
  });
  keyboard.text("Назад", "rating_menu");

  await ctx.reply("Выберите игрока.", { reply_markup: keyboard });
}

function parseSignedInteger(value: string) {
  const parsed = Number.parseInt(value.trim().replace("+", ""), 10);
  return Number.isInteger(parsed) ? parsed : null;
}

function parseRatingDate(value?: string) {
  if (!value) return undefined;
  const date = DateTime.fromISO(value.trim(), { zone: config.TAVERN_TIMEZONE });
  return date.isValid ? toUtcDate(date.startOf("day")) : undefined;
}

async function createGameFromDraft(ctx: Context, draft: Draft) {
  const id = userId(ctx);
  const master = await requireActiveMaster(ctx);
  if (!id || !master) return;

  const dateTime = validateDateTime(draft.date || "", draft.time || "");
  if (!dateTime.ok) {
    await ctx.reply(dateTime.message);
    return;
  }

  const dateTimeEnd = draft.durationMinutes ? dateTime.value.plus({ minutes: draft.durationMinutes }) : null;
  const game = await prisma.game.create({
    data: {
      masterId: master.id,
      title: draft.title!,
      description: draft.description!,
      gameSystem: draft.gameSystem!,
      experienceLevel: draft.experienceLevel!,
      ageRating: draft.ageRating!,
      dateTimeStart: toUtcDate(dateTime.value),
      dateTimeEnd: dateTimeEnd ? toUtcDate(dateTimeEnd) : null,
      durationMinutes: draft.durationMinutes,
      minPlayers: draft.minPlayers!,
      maxPlayers: draft.maxPlayers!,
      price: draft.price!,
      currency: draft.currency || "BYN",
      contactUrl: draft.contactOverride || master.contactUrl,
      status: config.AUTO_PUBLISH ? "published" : "pending",
      publishedAt: config.AUTO_PUBLISH ? new Date() : null
    }
  });

  await audit(String(id), "game.created", "Game", game.id, { autoPublish: config.AUTO_PUBLISH });
  await resetSession(id);
  await ctx.reply(
    config.AUTO_PUBLISH
      ? "Игра опубликована и добавлена в афишу."
      : "Заявка создана и ждёт подтверждения администратора.",
    { reply_markup: mainMenu(master.role === "admin") }
  );
}

async function handleText(ctx: Context) {
  const id = userId(ctx);
  const rawText = ctx.message && "text" in ctx.message ? ctx.message.text : "";
  const text = typeof rawText === "string" ? cleanText(rawText) : "";
  if (!id || !text) return;

  const session = await getSession(id);
  const draft = session.draft;

  try {
    if (session.state === "create:image") {
      await saveSession(id, "create:contact", draft);
      await ctx.reply("Изображения для афиши больше не нужны. Использовать контакт мастера для записи или указать другой?", {
        reply_markup: contactChoiceKeyboard()
      });
      return;
    }

    if (session.state === "register:name") {
      const displayName = gameText.title.parse(text);
      await askContact(ctx, { ...draft, displayName });
      return;
    }

    if (session.state === "register:contact") {
      const contactUrl = telegramContactSchema.parse(text);
      await confirmRegistration(ctx, { ...draft, contactUrl });
      return;
    }

    if (session.state === "create:title") {
      await saveSession(id, "create:date", { ...draft, title: gameText.title.parse(text) });
      await ctx.reply("Выберите дату проведения игры.", { reply_markup: dateKeyboard() });
      return;
    }

    if (session.state === "create:date_manual") {
      await saveSession(id, "create:time", { ...draft, date: text });
      await ctx.reply("Укажите время начала игры в формате ЧЧ:ММ, например 18:30.", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "create:time") {
      const check = validateDateTime(draft.date || "", text);
      if (!check.ok) return void (await ctx.reply(check.message));
      await saveSession(id, "create:duration", { ...draft, time: text });
      await ctx.reply("Укажите предполагаемую продолжительность игры.", { reply_markup: durationKeyboard() });
      return;
    }

    if (session.state === "create:duration_manual") {
      const hours = Number(text.replace(",", "."));
      if (!Number.isFinite(hours) || hours <= 0 || hours > 12) {
        await ctx.reply("Введите продолжительность в часах, например 3.5.");
        return;
      }
      await saveSession(id, "create:description", { ...draft, durationMinutes: Math.round(hours * 60) });
      await ctx.reply("Кратко расскажите, о чём будет эта игра.", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "create:description") {
      await saveSession(id, "create:players", { ...draft, description: gameText.description.parse(text) });
      await ctx.reply("Сколько игроков вы планируете принять? Можно указать диапазон, например 3-5.", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "create:players") {
      const players = parsePlayers(text);
      if (!players) return void (await ctx.reply("Введите целое число от 1 до 20 или диапазон, например 3-5."));
      await saveSession(id, "create:price", { ...draft, ...players });
      await ctx.reply("Укажите стоимость участия с одного человека, например 35 BYN. Для бесплатной игры: 0 BYN.", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "create:price") {
      const price = parsePrice(text);
      if (!price) return void (await ctx.reply("Стоимость не может быть отрицательной. Пример: 35 BYN."));
      await saveSession(id, "create:system", { ...draft, ...price });
      await ctx.reply("По какой игровой системе будет проходить игра?", {
        reply_markup: optionKeyboard("system", ["Dungeons & Dragons", "Vampire: The Masquerade", "Call of Cthulhu", "Собственная система"])
      });
      return;
    }

    if (session.state === "create:system_manual") {
      await saveSession(id, "create:experience", { ...draft, gameSystem: gameText.gameSystem.parse(text) });
      await ctx.reply("Для кого подходит игра?", {
        reply_markup: optionKeyboard("experience", ["без опыта", "для новичков", "любой уровень", "для опытных игроков"])
      });
      return;
    }

    if (session.state === "create:experience_manual") {
      await saveSession(id, "create:age", { ...draft, experienceLevel: gameText.experienceLevel.parse(text) });
      await ctx.reply("Выберите возрастное ограничение.", {
        reply_markup: optionKeyboard("age", ["12+", "14+", "16+", "18+", "без ограничения"])
      });
      return;
    }

    if (session.state === "create:age_manual") {
      await saveSession(id, "create:contact", { ...draft, ageRating: gameText.ageRating.parse(text) });
      await ctx.reply("Использовать контакт мастера для записи или указать другой?", {
        reply_markup: contactChoiceKeyboard()
      });
      return;
    }

    if (session.state === "create:contact_manual") {
      const contactOverride = telegramContactSchema.parse(text);
      const nextDraft = { ...draft, contactOverride };
      await saveSession(id, "create:preview", nextDraft);
      await ctx.reply(gamePreview(nextDraft), {
        reply_markup: new InlineKeyboard().text("Опубликовать", "confirm_game").row().text("Отмена", "cancel")
      });
      return;
    }

    if (session.state === "gallery:photo:title") {
      await saveSession(id, "gallery:photo:description", { ...draft, galleryTitle: gameText.title.parse(text) });
      await ctx.reply("Добавьте краткое описание публикации или напишите «нет».", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "gallery:photo:description") {
      await saveSession(id, "gallery:category", {
        ...draft,
        galleryDescription: /^нет$/i.test(text) ? undefined : text.slice(0, 700)
      });
      await ctx.reply("Выберите категорию публикации.", { reply_markup: galleryCategoryKeyboard() });
      return;
    }

    if (session.state === "gallery:story:title") {
      await saveSession(id, "gallery:story:content", { ...draft, galleryTitle: gameText.title.parse(text) });
      await ctx.reply("Введите текст истории. Можно использовать абзацы, списки, цитаты, **жирный** и *курсив*.", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "gallery:story:content") {
      if (text.length < 20) return void (await ctx.reply("История слишком короткая. Добавьте хотя бы пару предложений."));
      await saveSession(id, "gallery:story:description", { ...draft, galleryStoryContent: text.slice(0, 8000) });
      await ctx.reply("Добавьте краткое вступление для карточки или напишите «нет».", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "gallery:story:description") {
      await saveSession(id, "gallery:category", {
        ...draft,
        galleryDescription: /^нет$/i.test(text) ? undefined : text.slice(0, 700)
      });
      await ctx.reply("Выберите категорию истории.", { reply_markup: galleryCategoryKeyboard() });
      return;
    }

    if (session.state === "gallery:date") {
      const galleryEventDate = parseOptionalGalleryDate(text);
      const nextDraft = { ...draft, galleryEventDate };
      if (draft.galleryType === "story") {
        await saveSession(id, "gallery:story:media-choice", nextDraft);
        await ctx.reply("Хотите добавить фотографии к истории?", { reply_markup: galleryStoryMediaChoiceKeyboard() });
        return;
      }
      await saveSession(id, "gallery:publish", nextDraft);
      await ctx.reply("Опубликовать сразу или сохранить как черновик?", { reply_markup: galleryPublishKeyboard() });
      return;
    }

    if (session.state.startsWith("profile:")) {
      const master = await requireActiveMaster(ctx);
      if (!master) return;
      if (session.state === "profile:name") {
        await prisma.master.update({ where: { id: master.id }, data: { displayName: gameText.title.parse(text) } });
      } else {
        await prisma.master.update({ where: { id: master.id }, data: { contactUrl: telegramContactSchema.parse(text) } });
      }
      await resetSession(id);
      await ctx.reply("Профиль обновлён.");
      await showProfile(ctx);
      return;
    }

    if (session.state === "rating:create:name") {
      const master = await requireRatingAdmin(ctx);
      if (!master) return;
      await saveSession(id, "rating:create:nickname", { ratingDisplayName: gameText.title.parse(text) });
      await ctx.reply("Укажите игровой псевдоним игрока или напишите «нет».", { reply_markup: navKeyboard() });
      return;
    }

    if (session.state === "rating:create:nickname") {
      const master = await requireRatingAdmin(ctx);
      if (!master || !draft.ratingDisplayName) return;
      const nickname = /^нет$/i.test(text) ? undefined : gameText.title.parse(text);
      const player = await createRatingPlayer({
        displayName: draft.ratingDisplayName,
        nickname,
        createdByTelegramId: id,
        createdByMasterId: master.id
      });
      await resetSession(id);
      await ctx.reply(`Игрок добавлен в рейтинг: ${player.displayName}${player.nickname ? ` (${player.nickname})` : ""}.`, { reply_markup: ratingMenuKeyboard() });
      return;
    }

    if (session.state.startsWith("rating:game:")) {
      const master = await requireRatingAdmin(ctx);
      if (!master) return;
      const playerId = session.state.split(":")[2];
      const [pointsRaw, titleRaw, dateRaw, reasonRaw] = text.split(";").map((value) => value.trim());
      const points = parseSignedInteger(pointsRaw || "");
      if (points === null || points < 0 || !titleRaw) {
        await ctx.reply("Введите данные в формате: очки; название игры; дата YYYY-MM-DD (необязательно); комментарий (необязательно).");
        return;
      }
      await addRatingGameResult({
        playerId,
        points,
        gameTitle: titleRaw,
        gameDate: parseRatingDate(dateRaw),
        masterName: master.displayName,
        reason: reasonRaw || undefined,
        createdByTelegramId: id,
        createdByMasterId: master.id,
        idempotencyKey: `rating:game:${ctx.update.update_id}`
      });
      await resetSession(id);
      await ctx.reply("Сыгранная игра добавлена. Очки, количество игр, среднее и место пересчитаны.", { reply_markup: ratingMenuKeyboard() });
      return;
    }

    if (session.state.startsWith("rating:points:")) {
      const master = await requireRatingAdmin(ctx);
      if (!master) return;
      const playerId = session.state.split(":")[2];
      const [deltaRaw, reasonRaw] = text.split(";").map((value) => value.trim());
      const pointsDelta = parseSignedInteger(deltaRaw || "");
      if (pointsDelta === null || pointsDelta === 0 || !reasonRaw) {
        await ctx.reply("Введите данные в формате: +5; причина начисления или -5; причина исправления.");
        return;
      }
      await adjustRatingPoints({
        playerId,
        pointsDelta,
        reason: reasonRaw,
        createdByTelegramId: id,
        createdByMasterId: master.id,
        idempotencyKey: `rating:points:${ctx.update.update_id}`
      });
      await resetSession(id);
      await ctx.reply("Очки сохранены. Рейтинг пересчитан.", { reply_markup: ratingMenuKeyboard() });
      return;
    }

    if (session.state.startsWith("rating:inspiration:")) {
      const master = await requireRatingAdmin(ctx);
      if (!master) return;
      const playerId = session.state.split(":")[2];
      const [deltaRaw, reasonRaw] = text.split(";").map((value) => value.trim());
      const inspirationDelta = parseSignedInteger(deltaRaw || "");
      if (inspirationDelta === null || inspirationDelta === 0 || !reasonRaw) {
        await ctx.reply("Введите данные в формате: +1; причина вдохновения или -1; причина исправления.");
        return;
      }
      await adjustRatingInspiration({
        playerId,
        inspirationDelta,
        reason: reasonRaw,
        createdByTelegramId: id,
        createdByMasterId: master.id,
        idempotencyKey: `rating:inspiration:${ctx.update.update_id}`
      });
      await resetSession(id);
      await ctx.reply("Вдохновение сохранено. Рейтинг пересчитан.", { reply_markup: ratingMenuKeyboard() });
      return;
    }

    if (session.state.startsWith("edit:")) {
      const [, gameId, field] = session.state.split(":");
      const master = await requireActiveMaster(ctx);
      if (!master) return;
      const game = await prisma.game.findFirst({ where: { id: gameId, masterId: master.id } });
      if (!game) return void (await ctx.reply("Игра не найдена или принадлежит другому мастеру."));

      const data: Record<string, unknown> = {};
      if (field === "title") data.title = gameText.title.parse(text);
      if (field === "description") data.description = gameText.description.parse(text);
      if (field === "players") Object.assign(data, parsePlayers(text) || {});
      if (field === "price") Object.assign(data, parsePrice(text) || {});
      if (field === "contact") data.contactUrl = telegramContactSchema.parse(text);
      if (field === "time") {
        const currentDate = DateTime.fromJSDate(game.dateTimeStart, { zone: "utc" }).setZone(config.TAVERN_TIMEZONE).toISODate()!;
        const check = validateDateTime(currentDate, text);
        if (!check.ok) return void (await ctx.reply(check.message));
        data.dateTimeStart = toUtcDate(check.value);
      }
      if (!Object.keys(data).length) return void (await ctx.reply("Это поле пока нельзя изменить."));

      await prisma.game.update({ where: { id: game.id }, data });
      await audit(String(id), "game.updated", "Game", game.id, { field });
      await resetSession(id);
      await ctx.reply("Игра обновлена. Изменения появятся на сайте после следующего обновления афиши.");
      return;
    }

    await ctx.reply("Я сохранил текущий черновик. Выберите действие в меню.", { reply_markup: mainMenu() });
  } catch (error) {
    logger.warn({ error }, "bot validation error");
    await ctx.reply(error instanceof Error ? error.message : "Проверьте введённые данные.");
  }
}

async function handleGalleryMedia(ctx: Context) {
  const id = userId(ctx);
  if (!id || !ctx.message) return;
  const session = await getSession(id);
  if (session.state !== "gallery:photo:media" && session.state !== "gallery:story:media") return;

  const master = await requireActiveMaster(ctx);
  if (!master) return;

  let fileId = "";
  let source: "photo" | "document" = "photo";
  if ("photo" in ctx.message && ctx.message.photo?.length) {
    fileId = ctx.message.photo[ctx.message.photo.length - 1].file_id;
  } else if ("document" in ctx.message && ctx.message.document) {
    const mime = ctx.message.document.mime_type || "";
    if (!["image/jpeg", "image/png", "image/webp"].includes(mime)) {
      await ctx.reply("Документ должен быть изображением JPEG, PNG или WEBP.");
      return;
    }
    fileId = ctx.message.document.file_id;
    source = "document";
  }

  if (!fileId) return;
  const media = [...(session.draft.galleryMedia || []), { fileId, source }].slice(0, 20);
  await saveSession(id, session.state, { ...session.draft, galleryMedia: media });
  await ctx.reply(`Фото добавлено: ${media.length}. Можно отправить ещё или нажать «Далее».`, {
    reply_markup: galleryMediaKeyboard()
  });
}

async function handleCallback(ctx: Context) {
  const id = userId(ctx);
  const data = ctx.callbackQuery?.data;
  if (!id || !data) return;

  await ctx.answerCallbackQuery();
  const session = await getSession(id);
  const draft = session.draft;

  if (data === "cancel") {
    await resetSession(id);
    await ctx.reply("Действие отменено.", { reply_markup: startMenu() });
    return;
  }

  if (data === "menu" || data === "login") {
    const master = await findMaster(id);
    await resetSession(id);
    await ctx.reply(master ? "Главное меню мастера." : "Вы ещё не зарегистрированы.", {
      reply_markup: master ? mainMenu(master.role === "admin") : startMenu()
    });
    return;
  }

  if (data === "help") {
    await ctx.reply("Бот помогает мастерам зарегистрироваться, создать игру для афиши, посмотреть свои заявки, отредактировать или отменить игру. На каждом шаге можно нажать «Отмена» или вернуться в главное меню.");
    return;
  }

  if (data === "register") return startRegistration(ctx);
  if (data === "use_current_username") return confirmRegistration(ctx, { ...draft, contactUrl: `https://t.me/${ctx.from?.username}` });
  if (data === "manual_contact") {
    await ctx.reply("Введите контакт в формате @username или https://t.me/username.", { reply_markup: navKeyboard() });
    return;
  }
  if (data === "register_edit_name") return startRegistration(ctx);
  if (data === "register_edit_contact") return askContact(ctx, draft);
  if (data === "confirm_registration") return createMaster(ctx, draft);

  if (data === "games_menu") {
    const master = await requireActiveMaster(ctx);
    if (!master) return;
    await ctx.reply("Афиша и игры", { reply_markup: gamesMenuKeyboard() });
    return;
  }
  if (data === "create_game") return beginGameDraft(ctx);
  if (data === "my_games") return listGames(ctx, "all");
  if (data === "upcoming_games") return listGames(ctx, "upcoming");
  if (data === "past_games") return listGames(ctx, "past");
  if (data === "profile") return showProfile(ctx);

  if (data === "gallery_menu") return showGalleryMenu(ctx);
  if (data === "gallery_add_photo") return startGalleryPhoto(ctx);
  if (data === "gallery_add_story") return startGalleryStory(ctx);
  if (data === "gallery_posts") return listGalleryPosts(ctx);
  if (data === "gallery_media_done") {
    if (!draft.galleryMedia?.length && draft.galleryType === "photo") {
      await ctx.reply("Сначала отправьте хотя бы одну фотографию.");
      return;
    }
    await saveSession(id, draft.galleryType === "story" ? "gallery:publish" : "gallery:photo:title", draft);
    await ctx.reply(
      draft.galleryType === "story"
        ? "Опубликовать сразу или сохранить как черновик?"
        : "Введите название публикации.",
      { reply_markup: draft.galleryType === "story" ? galleryPublishKeyboard() : navKeyboard() }
    );
    return;
  }
  if (data === "gallery_story_add_media") {
    await saveSession(id, "gallery:story:media", draft);
    await ctx.reply("Отправьте фотографии для истории. Когда закончите, нажмите «Далее».", { reply_markup: galleryMediaKeyboard() });
    return;
  }
  if (data === "gallery_story_skip_media") {
    await saveSession(id, "gallery:publish", draft);
    await ctx.reply("Опубликовать сразу или сохранить как черновик?", { reply_markup: galleryPublishKeyboard() });
    return;
  }
  if (data.startsWith("gallery_category:")) {
    const category = data.slice(17) as Draft["galleryCategory"];
    await saveSession(id, "gallery:date", { ...draft, galleryCategory: category });
    await ctx.reply("Укажите дату события в формате YYYY-MM-DD или напишите «нет».", { reply_markup: navKeyboard() });
    return;
  }
  if (data === "gallery_publish" || data === "gallery_draft") {
    const status = data === "gallery_publish" ? "published" : "draft";
    await saveSession(id, `gallery:confirm:${status}`, draft);
    await ctx.reply(galleryDraftPreview(draft, status), { reply_markup: galleryConfirmKeyboard() });
    return;
  }
  if (data === "gallery_confirm") {
    const status = session.state.endsWith(":draft") ? "draft" : "published";
    return createGalleryFromDraft(ctx, draft, status);
  }
  if (data.startsWith("gallery_set_")) {
    const master = await requireActiveMaster(ctx);
    if (!master) return;
    const [statusPart, postId] = data.replace("gallery_set_", "").split(":");
    const status = statusPart === "hidden" ? "hidden" : "published";
    await setGalleryPostStatus({
      postId,
      masterId: master.id,
      isAdmin: isContentAdmin(ctx, master),
      status,
      createdByTelegramId: id
    });
    await ctx.reply(status === "hidden" ? "Публикация скрыта и не отображается на сайте." : "Публикация опубликована и отображается на сайте.", {
      reply_markup: galleryMenuKeyboard()
    });
    return;
  }

  if (data === "rating_menu") return showRatingMenu(ctx);
  if (data === "rating_players") return showRatingPlayers(ctx);
  if (data === "rating_history") return showRatingHistory(ctx);
  if (data === "rating_create_player") {
    const master = await requireRatingAdmin(ctx);
    if (!master) return;
    await saveSession(id, "rating:create:name", {});
    await ctx.reply("Введите публичное имя игрока для рейтинга.", { reply_markup: navKeyboard() });
    return;
  }
  if (data === "rating_add_game") return selectRatingPlayer(ctx, "game");
  if (data === "rating_adjust_points") return selectRatingPlayer(ctx, "points");
  if (data === "rating_adjust_inspiration") return selectRatingPlayer(ctx, "inspiration");
  if (data === "rating_visibility") return selectRatingPlayer(ctx, "visibility");
  if (data.startsWith("rating_game:")) {
    const master = await requireRatingAdmin(ctx);
    if (!master) return;
    await saveSession(id, `rating:game:${data.slice(12)}`, {});
    await ctx.reply("Введите результат в формате: очки; название игры; дата YYYY-MM-DD (необязательно); комментарий (необязательно).", { reply_markup: navKeyboard() });
    return;
  }
  if (data.startsWith("rating_points:")) {
    const master = await requireRatingAdmin(ctx);
    if (!master) return;
    await saveSession(id, `rating:points:${data.slice(14)}`, {});
    await ctx.reply("Введите изменение очков в формате: +5; причина или -5; причина исправления.", { reply_markup: navKeyboard() });
    return;
  }
  if (data.startsWith("rating_insp:")) {
    const master = await requireRatingAdmin(ctx);
    if (!master) return;
    await saveSession(id, `rating:inspiration:${data.slice(12)}`, {});
    await ctx.reply("Введите изменение вдохновения в формате: +1; причина или -1; причина исправления.", { reply_markup: navKeyboard() });
    return;
  }
  if (data.startsWith("rating_vis:")) {
    const master = await requireRatingAdmin(ctx);
    if (!master) return;
    const playerId = data.slice(11);
    const players = await listRatingPlayersForBot(true);
    const player = players.find((item) => item.id === playerId);
    if (!player) return void (await ctx.reply("Игрок не найден."));
    await setRatingPlayerVisibility({
      playerId,
      isVisible: !player.isVisible,
      reason: player.isVisible ? "Игрок скрыт из публичного рейтинга через Telegram-бота." : "Игрок возвращён в публичный рейтинг через Telegram-бота.",
      createdByTelegramId: id,
      createdByMasterId: master.id
    });
    await ctx.reply(player.isVisible ? "Игрок скрыт из публичного рейтинга." : "Игрок снова отображается в публичном рейтинге.", { reply_markup: ratingMenuKeyboard() });
    return;
  }

  if (data === "profile_edit_name") {
    await saveSession(id, "profile:name", {});
    await ctx.reply("Введите новое имя мастера.");
    return;
  }
  if (data === "profile_edit_contact") {
    await saveSession(id, "profile:contact", {});
    await ctx.reply("Введите новый Telegram-контакт.");
    return;
  }

  if (data.startsWith("date:")) {
    await saveSession(id, "create:time", { ...draft, date: data.slice(5) });
    await ctx.reply("Укажите время начала игры в формате ЧЧ:ММ, например 18:30.", { reply_markup: navKeyboard() });
    return;
  }
  if (data === "date_manual") {
    await saveSession(id, "create:date_manual", draft);
    await ctx.reply("Введите дату в формате ГГГГ-ММ-ДД.", { reply_markup: navKeyboard() });
    return;
  }
  if (data.startsWith("duration:")) {
    const duration = Number(data.slice(9));
    await saveSession(id, "create:description", { ...draft, durationMinutes: duration || undefined });
    await ctx.reply("Кратко расскажите, о чём будет эта игра.", { reply_markup: navKeyboard() });
    return;
  }
  if (data === "duration_manual") {
    await saveSession(id, "create:duration_manual", draft);
    await ctx.reply("Введите продолжительность в часах, например 3.5.", { reply_markup: navKeyboard() });
    return;
  }

  if (data.startsWith("system:")) {
    const value = data.slice(7);
    if (value === "manual") {
      await saveSession(id, "create:system_manual", draft);
      await ctx.reply("Введите игровую систему.");
      return;
    }
    await saveSession(id, "create:experience", { ...draft, gameSystem: value });
    await ctx.reply("Для кого подходит игра?", {
      reply_markup: optionKeyboard("experience", ["без опыта", "для новичков", "любой уровень", "для опытных игроков"])
    });
    return;
  }

  if (data.startsWith("experience:")) {
    const value = data.slice(11);
    if (value === "manual") {
      await saveSession(id, "create:experience_manual", draft);
      await ctx.reply("Введите уровень игроков.");
      return;
    }
    await saveSession(id, "create:age", { ...draft, experienceLevel: value });
    await ctx.reply("Выберите возрастное ограничение.", {
      reply_markup: optionKeyboard("age", ["12+", "14+", "16+", "18+", "без ограничения"])
    });
    return;
  }

  if (data.startsWith("age:")) {
    const value = data.slice(4);
    if (value === "manual") {
      await saveSession(id, "create:age_manual", draft);
      await ctx.reply("Введите возрастное ограничение.");
      return;
    }
    await saveSession(id, "create:contact", { ...draft, ageRating: value });
    await ctx.reply("Использовать контакт мастера для записи или указать другой?", {
      reply_markup: contactChoiceKeyboard()
    });
    return;
  }

  if (data === "use_profile_contact") {
    const nextDraft = { ...draft };
    await saveSession(id, "create:preview", nextDraft);
    await ctx.reply(gamePreview(nextDraft), {
      reply_markup: new InlineKeyboard().text("Опубликовать", "confirm_game").row().text("Отмена", "cancel")
    });
    return;
  }
  if (data === "manual_game_contact") {
    await saveSession(id, "create:contact_manual", draft);
    await ctx.reply("Введите контакт для записи в формате @username или https://t.me/username.");
    return;
  }
  if (data === "confirm_game") return createGameFromDraft(ctx, draft);

  if (data.startsWith("cancel_game:")) {
    const gameId = data.slice(12);
    await ctx.reply("Вы уверены, что хотите отменить игру? Она исчезнет из актуальной афиши.", {
      reply_markup: new InlineKeyboard().text("Да, отменить", `confirm_cancel:${gameId}`).row().text("Нет, оставить", "menu")
    });
    return;
  }
  if (data.startsWith("confirm_cancel:")) {
    const master = await requireActiveMaster(ctx);
    if (!master) return;
    const gameId = data.slice(15);
    const game = await prisma.game.findFirst({ where: { id: gameId, masterId: master.id } });
    if (!game) return void (await ctx.reply("Игра не найдена или принадлежит другому мастеру."));
    await prisma.game.update({ where: { id: game.id }, data: { status: "cancelled", cancelledAt: new Date() } });
    await audit(String(id), "game.cancelled", "Game", game.id);
    await ctx.reply("Игра отменена и больше не показывается в актуальной афише.", { reply_markup: mainMenu(master.role === "admin") });
    return;
  }

  if (data.startsWith("edit_game:")) {
    const gameId = data.slice(10);
    await ctx.reply("Что изменить?", {
      reply_markup: new InlineKeyboard()
        .text("Название", `edit_field:${gameId}:title`)
        .text("Время", `edit_field:${gameId}:time`).row()
        .text("Описание", `edit_field:${gameId}:description`)
        .text("Игроки", `edit_field:${gameId}:players`).row()
        .text("Стоимость", `edit_field:${gameId}:price`)
        .text("Контакт", `edit_field:${gameId}:contact`)
    });
    return;
  }
  if (data.startsWith("edit_field:")) {
    const [, gameId, field] = data.split(":");
    await saveSession(id, `edit:${gameId}:${field}`, {});
    await ctx.reply("Введите новое значение.");
    return;
  }

  if (data === "admin") {
    const master = await findMaster(id);
    if (!master || master.role !== "admin") return void (await ctx.reply("Раздел доступен только администраторам."));
    const [masters, games, pending] = await Promise.all([
      prisma.master.count(),
      prisma.game.count(),
      prisma.game.count({ where: { status: "pending" } })
    ]);
    await ctx.reply(`Администрирование\n\nМастеров: ${masters}\nИгр: ${games}\nОжидают модерации: ${pending}`);
  }
}

export async function startBot() {
  if (!config.TELEGRAM_BOT_TOKEN) {
    markBotDisabled(config.BOT_MODE);
    logger.warn("telegram bot token is empty; bot is disabled");
    return;
  }

  markBotStarting(config.BOT_MODE);
  const bot = new Bot(config.TELEGRAM_BOT_TOKEN);
  bot.use(async (ctx, next) => {
    markBotUpdate();
    logger.info({ updateId: ctx.update.update_id, type: Object.keys(ctx.update).find((key) => key !== "update_id") }, "telegram update received");
    await next();
  });
  bot.command("start", sendStart);
  bot.command("rating", showRatingMenu);
  bot.command("admin", async (ctx) => {
    const id = userId(ctx);
    if (!id) return;
    const master = await findMaster(id);
    if (!master || master.role !== "admin") {
      await ctx.reply("Раздел доступен только администраторам.");
      return;
    }
    const [masters, games, pending] = await Promise.all([
      prisma.master.count(),
      prisma.game.count(),
      prisma.game.count({ where: { status: "pending" } })
    ]);
    await ctx.reply(`Администрирование\n\nМастеров: ${masters}\nИгр: ${games}\nОжидают модерации: ${pending}`);
  });
  bot.on("callback_query:data", handleCallback);
  bot.on("message:photo", handleGalleryMedia);
  bot.on("message:document", handleGalleryMedia);
  bot.on("message:text", handleText);
  bot.catch((error) => {
    markBotError(error);
    logger.error({ error: botErrorDetails(error) }, "bot error");
  });

  if (config.BOT_MODE === "webhook" && config.WEBHOOK_URL) {
    try {
      await bot.api.setWebhook(config.WEBHOOK_URL);
      markBotRunning();
      logger.info("telegram webhook is configured; expose webhook handling on your production platform");
    } catch (error) {
      markBotError(error);
      logger.error({ error: botErrorDetails(error) }, "telegram webhook setup failed");
    }
    return;
  }

  try {
    await bot.api.deleteWebhook({ drop_pending_updates: false });
    logger.info("telegram webhook removed before polling start");
    run(bot);
    markBotRunning();
    logger.info("telegram bot is running in long polling mode");
  } catch (error) {
    markBotError(error);
    logger.error({ error: botErrorDetails(error) }, "telegram polling setup failed");
  }
}
