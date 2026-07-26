import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const html = readFileSync(resolve(root, "index.html"), "utf8");
const js = readFileSync(resolve(root, "src/main.js"), "utf8");
const css = readFileSync(resolve(root, "src/styles.css"), "utf8");

const requiredText = ["Личный кабинет Шляпника", "Программы мастера", "Desktop Agent", "Feature flags", "publicRegistration", "Код Шляпника", "Upload policy", "Колонки", "Export", "API_BASE", "Data:", "data-backend-output", "data-source-output", "data-action=\"create-game\"", "publish-game", "delete-game", "control-table-prefs", "control-runtime-config.js", "vendor/qrcode-bundle.js", "X-XSRF-TOKEN"];
const missing = requiredText.filter((text) => !html.includes(text) && !js.includes(text) && !css.includes(text));

if (missing.length) {
  throw new Error(`Missing required frontend text: ${missing.join(", ")}`);
}

if (!js.includes("localStorage.setItem(\"control-table-prefs\"")) {
  throw new Error("Table preference persistence smoke check failed.");
}

if (!js.includes("window.QRCode.toCanvas")) {
  throw new Error("Scannable QR renderer smoke check failed.");
}

if (js.includes("statusLine.innerHTML")) {
  throw new Error("Backend status updates must not replace the admin token input.");
}

if (css.includes("letter-spacing: -")) {
  throw new Error("Negative letter spacing is not allowed.");
}

console.log("frontend smoke checks passed");
