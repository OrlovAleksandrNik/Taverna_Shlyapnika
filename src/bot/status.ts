export type BotRuntimeState = {
  enabled: boolean;
  running: boolean;
  mode: "polling" | "webhook";
  startedAt: string | null;
  lastUpdateAt: string | null;
  lastError: string | null;
};

const state: BotRuntimeState = {
  enabled: false,
  running: false,
  mode: "polling",
  startedAt: null,
  lastUpdateAt: null,
  lastError: null
};

function redactSecrets(value: string) {
  return value
    .replace(/bot\d+:[A-Za-z0-9_-]+/g, "bot[redacted]")
    .replace(/\d{8,12}:[A-Za-z0-9_-]{30,}/g, "[redacted-token]");
}

export function markBotStarting(mode: "polling" | "webhook") {
  state.enabled = true;
  state.running = false;
  state.mode = mode;
  state.startedAt = new Date().toISOString();
  state.lastError = null;
}

export function markBotRunning() {
  state.running = true;
}

export function markBotDisabled(mode: "polling" | "webhook") {
  state.enabled = false;
  state.running = false;
  state.mode = mode;
  state.startedAt = null;
}

export function markBotUpdate() {
  state.lastUpdateAt = new Date().toISOString();
}

export function markBotError(error: unknown) {
  state.running = false;
  const message = error instanceof Error ? `${error.name}: ${error.message}` : String(error);
  state.lastError = redactSecrets(message);
}

export function getBotHealth() {
  return { ...state };
}
