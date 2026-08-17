import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const html = readFileSync(resolve(root, "index.html"), "utf8");
const js = readFileSync(resolve(root, "src/main.js"), "utf8");
const css = readFileSync(resolve(root, "src/styles.css"), "utf8");

const requiredText = [
  "Личный кабинет Шляпника",
  "Кабинет мастера",
  "Последние действия",
  "API_BASE",
  "data-action=\"create-game\"",
  "publish-game",
  "delete-game",
  "control-table-prefs",
  "control-runtime-config.js",
  "X-XSRF-TOKEN",
  "/api/auth/session",
  "/api/auth/logout",
  "/api/auth/session-mode"
];

const missing = requiredText.filter((text) => !html.includes(text) && !js.includes(text) && !css.includes(text));

if (missing.length) {
  throw new Error(`Missing required frontend text: ${missing.join(", ")}`);
}

if (css.includes("letter-spacing: -")) {
  throw new Error("Negative letter spacing is not allowed.");
}

const forbiddenText = [
  "Feature flags",
  "publicRegistration",
  "Upload policy",
  "Код действий",
  "Backend:",
  "DEVELOPER",
  "CONTENT_MANAGER",
  "RATING_MANAGER",
  "role-switch"
];

const forbidden = forbiddenText.filter((text) => html.includes(text) || js.includes(text) || css.includes(text));

if (forbidden.length) {
  throw new Error(`Forbidden legacy control UI text found: ${forbidden.join(", ")}`);
}

console.log("frontend smoke checks passed");
