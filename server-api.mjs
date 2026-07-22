import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { pathToFileURL, fileURLToPath } from "node:url";

const root = dirname(fileURLToPath(import.meta.url));
const apiEntry = join(root, "dist", "api", "server.js");

if (!existsSync(apiEntry)) {
  console.error("API bundle is missing. Run `pnpm run build` before starting the API.");
  process.exit(1);
}

const { startApi } = await import(pathToFileURL(apiEntry).href);
startApi();
