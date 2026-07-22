import { z } from "zod";
import { nowInTavernZone, parseLocalDateTime } from "./time.js";

export const telegramContactSchema = z
  .string()
  .trim()
  .refine((value) => /^@[A-Za-z0-9_]{5,32}$/.test(value) || /^https:\/\/t\.me\/[A-Za-z0-9_]{5,32}$/.test(value), {
    message: "Укажите контакт в формате @username или https://t.me/username."
  })
  .transform((value) => (value.startsWith("@") ? `https://t.me/${value.slice(1)}` : value));

export const gameText = {
  title: z.string().trim().min(3).max(100),
  description: z.string().trim().min(20).max(1000),
  gameSystem: z.string().trim().min(2).max(80),
  experienceLevel: z.string().trim().min(2).max(80),
  ageRating: z.string().trim().min(2).max(30)
};

export function cleanText(value: string) {
  return value.replace(/[<>]/g, "").replace(/\s+/g, " ").trim();
}

export function validateDateTime(date: string, time: string) {
  const value = parseLocalDateTime(date, time);
  if (!value) return { ok: false as const, message: "Введите дату и время в формате ГГГГ-ММ-ДД и ЧЧ:ММ." };
  if (value < nowInTavernZone()) return { ok: false as const, message: "Нельзя выбрать дату и время в прошлом." };
  return { ok: true as const, value };
}

export function parsePlayers(value: string) {
  const match = value.trim().match(/^(\d{1,2})(?:\s*[-–]\s*(\d{1,2}))?$/);
  if (!match) return null;

  const min = Number(match[1]);
  const max = Number(match[2] || match[1]);
  if (!Number.isInteger(min) || !Number.isInteger(max) || min < 1 || max > 20 || min > max) return null;
  return { minPlayers: min, maxPlayers: max };
}

export function parsePrice(value: string) {
  const match = value.trim().replace(",", ".").match(/^(\d+(?:\.\d{1,2})?)\s*([A-Za-zА-Яа-я]{3})?$/);
  if (!match) return null;

  const amount = Number(match[1]);
  if (!Number.isFinite(amount) || amount < 0) return null;
  return { price: amount.toFixed(2), currency: (match[2] || "BYN").toUpperCase() };
}
