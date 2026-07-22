import { spawnSync } from "node:child_process";
import { existsSync, readdirSync } from "node:fs";
import { join } from "node:path";
import process from "node:process";

const moduleDir = join(process.cwd(), "apps", "backend-java");
const wrapper = process.platform === "win32" ? "mvnw.cmd" : "./mvnw";
const wrapperPath = join(moduleDir, process.platform === "win32" ? "mvnw.cmd" : "mvnw");
const repoRoot = process.cwd();

if (!existsSync(wrapperPath)) {
  console.error(`Maven Wrapper was not found at ${wrapperPath}`);
  process.exit(1);
}

const javaHome = findJavaHome();
const env = { ...process.env };
if (javaHome && !env.JAVA_HOME) {
  env.JAVA_HOME = javaHome;
  env.PATH = join(javaHome, "bin") + (process.platform === "win32" ? ";" : ":") + (env.PATH || "");
}

const result = spawnSync(wrapper, process.argv.slice(2), {
  cwd: moduleDir,
  stdio: "inherit",
  shell: process.platform === "win32",
  env
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);

function findJavaHome() {
  if (process.env.JAVA_HOME && existsSync(join(process.env.JAVA_HOME, "bin", javaBinary()))) return process.env.JAVA_HOME;

  const local = join(repoRoot, ".tools", "jdk-21");
  if (existsSync(join(local, "bin", javaBinary()))) return local;

  const tools = join(repoRoot, ".tools");
  if (existsSync(tools)) {
    for (const name of readdirSync(tools)) {
      const candidate = join(tools, name);
      if (name.startsWith("jdk-") && existsSync(join(candidate, "bin", javaBinary()))) return candidate;
    }
  }

  const probe = spawnSync(process.platform === "win32" ? "where.exe" : "command", process.platform === "win32" ? ["java"] : ["-v", "java"], { stdio: "ignore", shell: false });
  if (probe.status === 0) return null;

  console.error("Java 21 was not found. Run: pnpm java:bootstrap");
  process.exit(1);
}

function javaBinary() {
  return process.platform === "win32" ? "java.exe" : "java";
}
