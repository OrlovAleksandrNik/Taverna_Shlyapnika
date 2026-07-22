import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { pathToFileURL, fileURLToPath } from "node:url";

const root = dirname(fileURLToPath(import.meta.url));
const distEntry = join(root, "dist", "index.js");

if (!existsSync(distEntry)) {
  console.error("Backend bundle is missing. Run `pnpm run build` before starting the site.");
  process.exit(1);
}

await import(pathToFileURL(distEntry).href);
