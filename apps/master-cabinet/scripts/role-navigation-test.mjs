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

assertIncludes("MASTER", "projects");
assertExcludes("MASTER", "users");
assertIncludes("CONTENT_MANAGER", "files");
assertExcludes("CONTENT_MANAGER", "users");
assertIncludes("DEVELOPER", "projects");
assertExcludes("DEVELOPER", "users");

if (!source.includes('roles.OWNER.splice(roles.OWNER.indexOf("settings"), 0, "security")')) {
  throw new Error("OWNER security menu wiring is missing");
}

console.log("role navigation checks passed");
