import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const source = readFileSync(resolve(import.meta.dirname, "../src/main.js"), "utf8");

function assertIncludes(needle, label = needle) {
  if (!source.includes(needle)) {
    throw new Error(`Missing action contract: ${label}`);
  }
}

const contracts = [
  ['apiPost("/api/v1/auth/login", body)', "login endpoint"],
  ['apiGet("/api/v1/auth/me")', "session endpoint"],
  ['apiPost("/api/v1/auth/password-reset", body)', "password reset endpoint"],
  ['apiPost("/api/v1/account/2fa/setup", {}, true)', "2FA setup endpoint with CSRF"],
  ['apiPost("/api/v1/account/2fa/confirm", body, true)', "2FA confirm endpoint with CSRF"],
  ['apiPost("/api/v1/account/2fa/disable", body, true)', "2FA disable endpoint with CSRF"],
  ['apiGet("/api/v1/account/sessions")', "account sessions endpoint"],
  ['apiPost("/api/v1/account/sessions/revoke-all", {}, true)', "session revoke endpoint with CSRF"],
  ['apiPost("/api/v1/admin/users/invitations", body, true)', "invitation endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/data/${section}`, { title, payload }, true)", "generic records endpoint with CSRF"],
  ['apiPost("/api/v1/admin/games", gamePayload(body), true)', "game creation endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/publish`, {}, true)", "game publish endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/cancel`, {}, true)", "game cancel endpoint with CSRF"],
  ["apiDelete(`/api/v1/admin/games/${sourceElement?.dataset.id}`)", "game delete endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/data/${section}/${sourceElement?.dataset.id}/publish`, {}, true)", "record publish endpoint with CSRF"],
  ["apiDelete(`/api/v1/admin/data/${section}/${sourceElement?.dataset.id}`)", "record delete endpoint with CSRF"],
  ["apiPost(`/api/v1/admin/projects/${projectCode}/launch`, {}, true)", "project launch endpoint with CSRF"],
  ['apiPost("/api/v1/admin/backups", {}, true)', "backup creation endpoint with CSRF"],
  ['apiPost("/api/v1/admin/backups/restore", {}, true)', "restore-blocked endpoint with CSRF"]
];

for (const [needle, label] of contracts) {
  assertIncludes(needle, label);
}

assertIncludes('headers["X-XSRF-TOKEN"] = await ensureCsrf()', "CSRF header");
assertIncludes('"X-XSRF-TOKEN": await ensureCsrf()', "DELETE CSRF header");
assertIncludes("new Date(body.startsAt).toISOString()", "game date normalization");
assertIncludes("state.actionStatus = actionErrorMessage(action, error)", "action failure feedback");

console.log("action contract checks passed");
