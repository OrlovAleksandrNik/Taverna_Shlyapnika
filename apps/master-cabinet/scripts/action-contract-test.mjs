import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const source = readFileSync(resolve(import.meta.dirname, "../src/main.js"), "utf8");

function assertIncludes(needle, label = needle) {
  if (!source.includes(needle)) {
    throw new Error(`Missing action contract: ${label}`);
  }
}

const contracts = [
  ['apiPost("/api/v1/admin/games", gamePayload(body), true)', "game creation endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/publish`, {}, true)", "game publish endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/cancel`, {}, true)", "game cancel endpoint with CSRF"],
  ["apiDelete(`/api/v1/admin/games/${sourceElement?.dataset.id}`)", "game delete endpoint with CSRF"]
];

for (const [needle, label] of contracts) {
  assertIncludes(needle, label);
}

assertIncludes('headers["X-XSRF-TOKEN"] = await ensureCsrf()', "CSRF header");
assertIncludes('"X-XSRF-TOKEN": await ensureCsrf()', "DELETE CSRF header");
assertIncludes("new Date(body.startsAt).toISOString()", "game date normalization");
assertIncludes("state.actionStatus = actionErrorMessage(action, error)", "action failure feedback");

console.log("action contract checks passed");
