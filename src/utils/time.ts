import { DateTime } from "luxon";
import { config } from "../config.js";

export function nowInTavernZone() {
  return DateTime.now().setZone(config.TAVERN_TIMEZONE);
}

export function parseLocalDateTime(date: string, time: string) {
  const value = DateTime.fromFormat(`${date} ${time}`, "yyyy-MM-dd HH:mm", {
    zone: config.TAVERN_TIMEZONE
  });

  if (!value.isValid) return null;
  return value;
}

export function toUtcDate(dateTime: DateTime) {
  return dateTime.toUTC().toJSDate();
}

export function formatForTelegram(date: Date) {
  return DateTime.fromJSDate(date, { zone: "utc" })
    .setZone(config.TAVERN_TIMEZONE)
    .setLocale("ru")
    .toFormat("dd.LL.yyyy, HH:mm");
}

export function formatForSite(date: Date) {
  return DateTime.fromJSDate(date, { zone: "utc" })
    .setZone(config.TAVERN_TIMEZONE)
    .setLocale("ru")
    .toFormat("ccc, dd LLL, HH:mm");
}
