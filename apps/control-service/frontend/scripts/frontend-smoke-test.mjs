import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const html = readFileSync(resolve(root, "index.html"), "utf8");
const js = readFileSync(resolve(root, "src/main.js"), "utf8");
const css = readFileSync(resolve(root, "src/styles.css"), "utf8");

const requiredText = ["Taverna Control", "Программы мастера", "Desktop Agent", "PUBLIC_REGISTRATION_ENABLED", "hashed backup codes"];
const missing = requiredText.filter((text) => !html.includes(text) && !js.includes(text) && !css.includes(text));

if (missing.length) {
  throw new Error(`Missing required frontend text: ${missing.join(", ")}`);
}

if (!js.includes("localStorage.setItem(\"control-story-draft\"")) {
  throw new Error("Autosave smoke check failed.");
}

if (css.includes("letter-spacing: -")) {
  throw new Error("Negative letter spacing is not allowed.");
}

console.log("frontend smoke checks passed");
