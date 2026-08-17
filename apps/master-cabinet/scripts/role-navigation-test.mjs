import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const source = readFileSync(resolve(import.meta.dirname, "../src/main.js"), "utf8");

function roleItems(role) {
  const match = new RegExp(`${role}:\\s*\\[([^\\]]+)\\]`, "s").exec(source);
  if (!match) throw new Error(`Role is not declared: ${role}`);
  return [...match[1].matchAll(/"([^"]+)"/g)].map((item) => item[1]);
}

function assertIncludes(role, item) {
  if (!roleItems(role).includes(item)) {
    throw new Error(`${role} must include ${item}`);
  }
}

function assertExcludes(role, item) {
  if (roleItems(role).includes(item)) {
    throw new Error(`${role} must not include ${item}`);
  }
}

assertIncludes("MASTER", "overview");
assertIncludes("MASTER", "games");
assertIncludes("MASTER", "rating");
assertIncludes("MASTER", "profile");
assertExcludes("MASTER", "masters");
assertExcludes("MASTER", "backups");
assertExcludes("MASTER", "settings");
assertIncludes("HATTER", "masters");
assertIncludes("HATTER", "profile");
assertExcludes("HATTER", "backups");
assertExcludes("HATTER", "settings");

for (const forbiddenRole of ["OWNER", "DEVELOPER", "CONTENT_MANAGER", "RATING_MANAGER", "SUPERADMIN", "VIEWER"]) {
  if (source.includes(`${forbiddenRole}:`)) {
    throw new Error(`Legacy role must not be declared: ${forbiddenRole}`);
  }
}

console.log("role navigation checks passed");
