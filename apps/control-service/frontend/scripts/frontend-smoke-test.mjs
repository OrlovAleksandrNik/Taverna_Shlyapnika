import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const html = readFileSync(resolve(root, "index.html"), "utf8");
const js = readFileSync(resolve(root, "src/main.js"), "utf8");
const css = readFileSync(resolve(root, "src/styles.css"), "utf8");

const requiredText = ["Taverna Control", "Программы мастера", "Desktop Agent", "Feature flags", "publicRegistration", "hashed backup codes", "Upload policy", "Колонки", "Export", "API_BASE", "Data:", "data-action=\"login\"", "data-action=\"create-record\"", "publish-record", "data-action=\"create-game\"", "publish-game", "delete-game", "data-action=\"project-launch\"", "data-action=\"backup-create\"", "data-action=\"2fa-setup\"", "control-table-prefs", "control-runtime-config.js", "vendor/qrcode-bundle.js", "X-XSRF-TOKEN"];
const missing = requiredText.filter((text) => !html.includes(text) && !js.includes(text) && !css.includes(text));

if (missing.length) {
  throw new Error(`Missing required frontend text: ${missing.join(", ")}`);
}

if (!js.includes("localStorage.setItem(\"control-story-draft\"")) {
  throw new Error("Autosave smoke check failed.");
}

if (!js.includes("localStorage.setItem(\"control-table-prefs\"")) {
  throw new Error("Table preference persistence smoke check failed.");
}

if (!js.includes("window.QRCode.toCanvas")) {
  throw new Error("Scannable QR renderer smoke check failed.");
}

if (css.includes("letter-spacing: -")) {
  throw new Error("Negative letter spacing is not allowed.");
}

console.log("frontend smoke checks passed");
