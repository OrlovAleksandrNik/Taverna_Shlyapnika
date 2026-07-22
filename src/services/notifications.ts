import { config, adminTelegramIds } from "../config.js";
import { logger } from "../logger.js";

type NotifyTarget = string | bigint | number | null | undefined;

function safeError(error: unknown) {
  if (error instanceof Error) return { name: error.name, message: error.message };
  return { message: String(error) };
}

export async function notifyTelegram(target: NotifyTarget, text: string) {
  if (!config.TELEGRAM_BOT_TOKEN || target === undefined || target === null || String(target).trim() === "") return false;

  try {
    const response = await fetch(`https://api.telegram.org/bot${config.TELEGRAM_BOT_TOKEN}/sendMessage`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: String(target),
        text,
        parse_mode: "HTML",
        disable_web_page_preview: true
      })
    });

    if (!response.ok) {
      logger.warn({ status: response.status }, "telegram notification failed");
      return false;
    }

    return true;
  } catch (error) {
    logger.warn({ error: safeError(error) }, "telegram notification error");
    return false;
  }
}

export async function notifyAdmins(text: string) {
  const ids = Array.from(adminTelegramIds);
  await Promise.all(ids.map((id) => notifyTelegram(id, text)));
}
