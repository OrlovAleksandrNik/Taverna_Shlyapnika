const API_BASE = "http://localhost:4190";

const roles = {
  OWNER: [
    "overview", "schedule", "games", "applications", "services", "masters", "players", "rating",
    "gallery", "stories", "files", "projects", "notifications", "backups", "audit", "users", "settings", "tech"
  ],
  MASTER: ["overview", "schedule", "games", "gallery", "stories", "projects", "notifications", "tech"],
  CONTENT_MANAGER: ["overview", "gallery", "stories", "files", "notifications", "tech"],
  RATING_MANAGER: ["overview", "players", "rating", "audit", "tech"],
  DEVELOPER: ["overview", "projects", "audit", "settings", "tech"]
};

const sections = {
  overview: ["Обзор", "Пульс кабинета"],
  schedule: ["Расписание", "Календарь и переносы"],
  games: ["Игры", "Редактор, публикация, статусы"],
  applications: ["Заявки", "Входящие обращения"],
  services: ["Услуги", "Пакеты и стоимость"],
  masters: ["Мастера", "Профили и назначения"],
  players: ["Игроки", "Карточки игроков"],
  rating: ["Рейтинг", "Операции и откаты"],
  gallery: ["Галерея", "Медиа и публикации"],
  stories: ["Истории", "Черновики и автосохранение"],
  files: ["Файлы", "Безопасное хранилище"],
  projects: ["Программы мастера", "VoiceMod и ScreenStage"],
  notifications: ["Уведомления", "Шаблоны и события"],
  backups: ["Резервные копии", "Создание и проверка"],
  audit: ["Журнал действий", "Критические операции"],
  users: ["Пользователи и роли", "Приглашения и permissions"],
  security: ["Безопасность", "2FA, сессии, восстановление"],
  settings: ["Настройки", "Флаги интеграций"],
  tech: ["Состояние", "Backend health"]
};

roles.OWNER.splice(roles.OWNER.indexOf("settings"), 0, "security");
roles.DEVELOPER.splice(roles.DEVELOPER.indexOf("settings"), 0, "security");

const state = {
  role: "OWNER",
  section: "overview",
  backend: "unknown",
  autosave: "saved",
  loadingSection: null,
  account: null,
  twoFactorSetup: null,
  tablePrefs: loadTablePrefs(),
  actionStatus: "Ready",
  remote: {}
};

const genericDataSections = new Set(["applications", "services", "masters", "players", "rating", "gallery", "stories", "notifications"]);

const mock = {
  metrics: [
    ["Ближайшие игры", "7", "2 сегодня"],
    ["Заявки", "12", "4 требуют ответа"],
    ["Активные мастера", "5", "1 ждёт подтверждения"],
    ["Черновики", "9", "3 давно не менялись"],
    ["Новые фото", "18", "mock gallery"],
    ["Ошибки интеграций", "0", "интеграции выключены"]
  ],
  games: [
    ["Тайна янтарного ключа", "26.07 19:00", "Станислав", "published"],
    ["Безумное чаепитие", "28.07 18:30", "Андрей", "draft"],
    ["Архивист и луна", "31.07 20:00", "Александр", "review"]
  ],
  audit: [
    ["OWNER", "users.invite", "MASTER", "сегодня 12:11"],
    ["MANAGER", "games.publish", "Тайна янтарного ключа", "сегодня 12:40"],
    ["DEVELOPER", "projects.mock_launch", "ScreenStage", "сегодня 13:02"]
  ],
  projects: [
    ["VoiceMod Panel", "Node.js ESM + static UI", "../../voicemod-panel-d-work", "mock-ready"],
    ["ScreenStage", ".NET 8 WPF + LibVLCSharp", "../../ScreenStage-redesign", "mock-ready"]
  ]
};

const app = document.querySelector("#app");

function render() {
  const visible = roles[state.role];
  const dataSource = sectionDataSource(state.section);
  app.innerHTML = `
    <div class="shell">
      <aside class="sidebar" aria-label="Разделы кабинета">
        <div class="brand">
          <img src="/hatter-mark.png" alt="" />
          <div>
            <b>Taverna Control</b>
            <span>Кабинет Мастера</span>
          </div>
        </div>
        <label class="role-switch">
          <span>Роль</span>
          <select id="roleSelect" aria-label="Выбор роли">
            ${Object.keys(roles).map((role) => `<option ${role === state.role ? "selected" : ""}>${role}</option>`).join("")}
          </select>
        </label>
        <nav>
          ${visible.map((key) => `
            <button class="nav-item ${state.section === key ? "active" : ""}" data-section="${key}" title="${sections[key][1]}">
              <span>${sections[key][0]}</span>
            </button>
          `).join("")}
        </nav>
      </aside>
      <main class="workspace">
        <header class="topbar">
          <div>
            <p class="eyebrow">Изолированный микросервис</p>
            <h1>${sections[state.section][0]}</h1>
            <p class="session-line">${escapeHtml(sessionLabel())}</p>
          </div>
          <div class="status-line" aria-live="polite">
            <span><span class="dot ${state.backend === "online" ? "ok" : ""}"></span>Backend: ${state.backend}</span>
            <span class="source-badge ${dataSource}">Data: ${dataSource}</span>
          </div>
        </header>
        <section class="content">
          <div class="action-status" aria-live="polite">${escapeHtml(state.actionStatus)}</div>
          ${sectionTemplate(state.section)}
        </section>
      </main>
      <aside class="notices" aria-label="Уведомления">
        <h2>Уведомления</h2>
        <button class="notice">Backup manifest создан в mock-хранилище</button>
        <button class="notice warn">Desktop Agent выключен</button>
        <button class="notice">CSRF cookie ожидается от backend</button>
      </aside>
    </div>
  `;

  document.querySelector("#roleSelect").addEventListener("change", (event) => {
    state.role = event.target.value;
    if (!roles[state.role].includes(state.section)) state.section = roles[state.role][0];
    render();
    loadSectionData(state.section);
  });
  document.querySelectorAll("[data-section]").forEach((button) => {
    button.addEventListener("click", () => {
      state.section = button.dataset.section;
      render();
      loadSectionData(state.section);
    });
  });
  document.querySelectorAll("[data-page-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const table = button.closest("[data-table-key]")?.dataset.tableKey;
      if (!table) return;
      const current = Number(state.tablePrefs[table]?.page || 1);
      updateTablePref(table, "page", button.dataset.pageAction === "next" ? current + 1 : current - 1);
    });
  });
  document.querySelectorAll("[data-danger]").forEach((button) => {
    button.addEventListener("click", () => {
      button.textContent = button.dataset.confirmed ? "Архивировать выбранные" : "Подтвердить";
      button.dataset.confirmed = "true";
    });
  });
  document.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", () => runAction(button.dataset.action, button.closest("form"), button));
  });
  const draft = document.querySelector("#draft");
  if (draft) {
    let timer;
    draft.addEventListener("input", () => {
      state.autosave = "saving";
      updateAutosave();
      clearTimeout(timer);
      timer = setTimeout(() => {
        localStorage.setItem("control-story-draft", draft.value);
        state.autosave = "saved";
        updateAutosave();
      }, 500);
    });
    draft.value = localStorage.getItem("control-story-draft") || "";
  }
  bindTableControls();
  renderQrCanvases();
  updateBackendStatus();
}

function sectionTemplate(section) {
  if (section === "overview") return overviewTemplate();
  if (section === "games" || section === "schedule") return gamesTemplate(section);
  if (section === "projects") return projectsTemplate();
  if (section === "users") return usersTemplate();
  if (section === "security") return securityTemplate();
  if (section === "files") return filesTemplate();
  if (section === "stories") return storiesTemplate();
  if (section === "settings") return settingsTemplate();
  if (section === "tech") return techTemplate();
  if (section === "audit") return auditTemplate();
  if (section === "backups") return backupsTemplate();
  return genericTemplate(section);
}

function overviewTemplate() {
  const snapshot = state.remote.overview;
  const metrics = snapshot?.metrics?.map((metric) => [
    metric.label,
    metric.value,
    metric.tone || snapshot.source || "backend"
  ]) || mock.metrics;
  const games = snapshot?.upcomingGames?.map((game) => [
    game.title,
    formatDateTime(game.startsAt),
    game.master,
    game.status
  ]) || mock.games;
  const actions = snapshot?.recentActions?.map((action) => [
    action.actor,
    action.action,
    action.entity,
    snapshot.generatedAt ? formatDateTime(snapshot.generatedAt) : "backend"
  ]) || [
    ["Мария", "game_result", "+12", "applied"],
    ["Илья", "correction", "-2", "needs review"]
  ];
  return `
    <div class="metric-grid">
      ${metrics.map(([label, value, hint]) => `<article class="metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong><small>${escapeHtml(hint)}</small></article>`).join("")}
    </div>
    ${tableTemplate("Ближайшие игры", ["Игра", "Дата", "Мастер", "Статус"], games)}
    ${tableTemplate("Последние операции", ["Кто", "Операция", "Объект", "Когда"], actions)}
  `;
}

function projectsTemplate() {
  const projects = state.remote.projects?.map((project) => [
    project.name || project.code,
    project.stack || project.kind,
    project.detectedPath || project.code,
    project.status || project.launchMode,
    project.code
  ]) || mock.projects.map((project, index) => [...project, index === 0 ? "voicemod" : "screenstage"]);
  return `
    <div class="project-grid">
      ${projects.map(([name, stack, path, status, code]) => `
        <article class="project">
          <h3>${escapeHtml(name)}</h3>
          <p>${escapeHtml(stack)}</p>
          <code>${escapeHtml(path)}</code>
          <span>${escapeHtml(status)}</span>
          <label>Назначить мастеру<input type="text" value="usr_mock_master" aria-label="Public ID мастера" /></label>
          <button data-action="project-launch" data-project="${escapeHtml(code)}" title="Создать запись mock-launch без запуска программы">Mock launch</button>
        </article>
      `).join("")}
    </div>
    <p class="note">Реальный запуск требует Desktop Agent, service auth и allowlist. Frontend не передаёт путь запуска.</p>
  `;
}

function usersTemplate() {
  const accountRows = toRows(state.remote.users, ["publicId", "email", "roles", "status"], [
    ["usr_owner_mock", "owner@example.test", "OWNER", "active"],
    ["usr_master_mock", "master@example.test", "MASTER", "invited"]
  ]);
  return `
    <form class="form-panel">
      <label>Email<input name="email" type="email" value="master@example.test" /></label>
      <label>Имя<input name="displayName" type="text" value="Новый мастер" /></label>
      <label>Роль<select name="role"><option>MASTER</option><option>CONTENT_MANAGER</option><option>RATING_MANAGER</option></select></label>
      <button type="button" data-action="invite-user" title="Создать одноразовое приглашение">Создать приглашение</button>
    </form>
    ${tableTemplate("Роли", ["Роль", "Смысл", "2FA"], [
      ["OWNER", "Полный доступ, последнего удалить нельзя", "required"],
      ["SUPERADMIN", "Администрирование системы", "required"],
      ["MASTER", "Свои игры, расписание, программы", "optional"]
    ])}
    ${tableTemplate("Операции аккаунтов", ["Операция", "Permission", "Audit", "Защита"], [
      ["Назначить роли", "users.assign_roles", "yes", "последний OWNER защищён"],
      ["Блокировать", "users.block", "yes", "последний OWNER защищён"],
      ["Деактивировать", "users.update", "yes", "soft state"],
      ["Удалить", "users.update", "yes", "soft delete"]
    ])}
    ${tableTemplate("Accounts", ["ID", "Email", "Roles", "Status"], accountRows)}
  `;
}

function securityTemplate() {
  const sessionRows = toRows(state.remote.securitySessions, ["userAgent", "ipAddress", "createdAt", "revokedAt"], [
    ["Windows Edge", "127.0.0.1", "сегодня", "active"],
    ["Tablet Safari", "10.0.0.14", "вчера", "revoked"]
  ]).map(([userAgent, ipAddress, createdAt, revokedAt]) => [
    userAgent,
    ipAddress,
    formatDateTime(createdAt),
    revokedAt ? `revoked ${formatDateTime(revokedAt)}` : "active"
  ]);
  const setup = state.twoFactorSetup;
  return `
    <form class="form-panel">
      <label>Email<input name="email" type="email" value="owner@example.test" autocomplete="username" /></label>
      <label>Пароль<input name="password" type="password" autocomplete="current-password" placeholder="CONTROL_BOOTSTRAP_OWNER_PASSWORD" /></label>
      <label>2FA code<input name="twoFactorCode" type="text" inputmode="numeric" placeholder="если включено" /></label>
      <div class="actions">
        <button type="button" data-action="login" title="Войти в isolated control backend">Войти</button>
        <button type="button" data-action="me" title="Проверить текущую session cookie">Проверить сессию</button>
      </div>
    </form>
    <div class="metric-grid">
      <article class="metric"><span>2FA OWNER/SUPERADMIN</span><strong>required</strong><small>TOTP + hashed backup codes</small></article>
      <article class="metric"><span>Сессии</span><strong>HttpOnly</strong><small>CONTROLSESSION, SameSite Strict</small></article>
      <article class="metric"><span>Reset tokens</span><strong>hashed</strong><small>одинаковый ответ без user enumeration</small></article>
    </div>
    <form class="form-panel">
      <label>Email для reset<input name="email" type="email" value="master@example.test" /></label>
      <button type="button" data-action="password-reset" title="Отправить mock password reset email">Запросить восстановление</button>
    </form>
    <form class="form-panel">
      <div class="actions">
        <button type="button" data-action="2fa-setup" title="Получить TOTP secret и QR">Создать 2FA QR</button>
        <button type="button" data-action="sessions-refresh" title="Загрузить активные backend-сессии">Обновить сессии</button>
        <button class="danger" type="button" data-action="sessions-revoke" title="Отозвать все backend-сессии">Отозвать все сессии</button>
      </div>
      ${setup ? `
        <div class="totp-setup">
          <canvas class="totp-qr" width="164" height="164" data-qr="${escapeHtml(setup.otpauthUrl)}" aria-label="TOTP QR"></canvas>
          <label>Secret<input readonly value="${escapeHtml(setup.secret)}" /></label>
        </div>
      ` : ""}
    </form>
    <form class="form-panel">
      <label>Код из приложения<input name="code" type="text" inputmode="numeric" placeholder="123456" /></label>
      <div class="actions">
        <button type="button" data-action="2fa-confirm" title="Включить TOTP и получить backup codes">Подтвердить 2FA</button>
      </div>
    </form>
    <form class="form-panel">
      <label>Пароль<input name="password" type="password" autocomplete="current-password" /></label>
      <label>2FA или backup code<input name="code" type="text" /></label>
      <button class="danger" type="button" data-action="2fa-disable" title="Отключить TOTP">Отключить 2FA</button>
    </form>
    ${tableTemplate("Активные устройства", ["Устройство", "IP", "Создано", "Статус"], sessionRows)}
  `;
}

function filesTemplate() {
  const storage = state.remote.filesStorage;
  const fileRows = storage ? [
    ["media", storage.media?.adapter, storage.media?.root, "configured"],
    ["projectArtifacts", storage.projectArtifacts?.adapter, storage.projectArtifacts?.root, "configured"],
    ["futureAdapters", Array.isArray(storage.futureAdapters) ? storage.futureAdapters.join(", ") : "", "planned"]
  ] : [
    ["images", ".jpg .png .webp .gif", "allowed", "проверять dimensions"],
    ["documents", ".pdf .txt .md", "allowed", "без секретов"],
    ["executables", ".exe .bat .cmd .ps1 .msi .dll .jar", "blocked", "только Desktop Agent allowlist"]
  ];
  return `
    <div class="metric-grid">
      <article class="metric"><span>MediaStorage</span><strong>${escapeHtml(storage?.media?.adapter || "local")}</strong><small>CONTROL_MEDIA_STORAGE_ROOT</small></article>
      <article class="metric"><span>ProjectArtifactStorage</span><strong>${escapeHtml(storage?.projectArtifacts?.adapter || "local")}</strong><small>не для загрузки .exe из UI</small></article>
      <article class="metric"><span>Upload policy</span><strong>strict</strong><small>MIME, extension, size, safe name</small></article>
    </div>
    ${tableTemplate("Политика файлов", ["Тип", "Правило", "Статус", "Заметка"], fileRows)}
  `;
}

function storiesTemplate() {
  return `
    <div class="editor">
      <label>Черновик истории<textarea id="draft" rows="8" placeholder="Текст сохраняется локально как mock автосохранение"></textarea></label>
      <span id="autosave">${state.autosave === "saved" ? "Сохранено" : "Сохраняю..."}</span>
    </div>
  `;
}

function backupsTemplate() {
  const rows = toRows(state.remote.backups, ["publicId", "status", "checksum", "manifestPath"], [
    ["bkp_mock_01", "COMPLETED", "checksum ok", "disabled"],
    ["bkp_mock_02", "PLANNED", "not started", "disabled"]
  ]);
  return `
    <div class="actions">
      <button data-action="backup-create" title="Создать manifest backup в локальном storage">Создать backup</button>
      <button class="danger" data-action="backup-restore" title="Восстановление требует отдельного подтверждения">Восстановление выключено</button>
    </div>
    ${tableTemplate("Backup jobs", ["ID", "Статус", "Проверка", "Manifest"], rows)}
  `;
}

function auditTemplate() {
  const rows = toRows(state.remote.audit, ["actorPublicId", "action", "entityType", "createdAt"], mock.audit)
    .map(([actor, action, entity, createdAt]) => [actor, action, entity, formatDateTime(createdAt)]);
  return tableTemplate("Последние действия", ["Кто", "Операция", "Объект", "Когда"], rows);
}

function settingsTemplate() {
  const flags = state.remote.settings?.featureFlags || {
    publicRegistration: false,
    mainSiteIntegration: false,
    telegramIntegration: false,
    desktopAgent: false
  };
  const rows = Object.entries(flags).map(([key, value]) => [key, String(value), value ? "enabled" : "disabled"]);
  const stored = toRows(state.remote.settings?.storedSettings, ["key", "value", "sensitive", "encrypted"], [
    ["mock.theme", "tavern", "false", "false"],
    ["mock.integration.mode", "isolated", "false", "false"]
  ]);
  return `
    ${tableTemplate("Feature flags", ["Key", "Value", "Status"], rows)}
    ${tableTemplate("Stored settings", ["Key", "Value", "Sensitive", "Encrypted"], stored)}
  `;
}

function techTemplate() {
  const integration = state.remote.tech || {
    mode: "isolated",
    mainSiteIntegrationEnabled: false,
    telegramIntegrationEnabled: false,
    desktopAgentEnabled: false,
    contractsPrepared: true,
    productionDataUsed: false
  };
  const desktopAgent = integration.desktopAgent || {
    enabled: integration.desktopAgentEnabled,
    allowlistedProjects: {},
    browserPathInputAccepted: false,
    allowedExtensions: ".exe, .cmd, .bat"
  };
  return `
    <div class="tech">
      <p>Backend health: <strong>${state.backend}</strong></p>
      <p>Integration mode: <strong>${escapeHtml(integration.mode)}</strong></p>
      <p>Main site integration: <strong>${escapeHtml(integration.mainSiteIntegrationEnabled)}</strong></p>
      <p>Telegram integration: <strong>${escapeHtml(integration.telegramIntegrationEnabled)}</strong></p>
      <p>Desktop Agent: <strong>${escapeHtml(integration.desktopAgentEnabled)}</strong></p>
      <p>Desktop allowlist: <strong>${escapeHtml(Object.keys(desktopAgent.allowlistedProjects || {}).join(", ") || "not configured")}</strong></p>
      <p>Browser path input: <strong>${escapeHtml(desktopAgent.browserPathInputAccepted)}</strong></p>
      <p>Contracts prepared: <strong>${escapeHtml(integration.contractsPrepared)}</strong></p>
      <p>Production data used: <strong>${escapeHtml(integration.productionDataUsed)}</strong></p>
    </div>
  `;
}

function gamesTemplate(section) {
  const fallback = section === "games" ? mock.games : mock.games.map((row) => [row[0], row[1], row[2], row[3]]);
  const records = recordsFromPayload(state.remote[section]);
  const rows = records.length ? records.map((game) => [
    game.title,
    formatDateTime(game.startsAt),
    game.masterPublicId || "unassigned",
    game.status,
    actionButtons([
      ["publish-game", "Опубликовать", game.id],
      ["cancel-game", "Отменить", game.id],
      ["delete-game", "Удалить", game.id]
    ])
  ]) : fallback.map(([title, startsAt, master, status]) => [
    title,
    formatDateTime(startsAt),
    master || "unassigned",
    status,
    actionButtons([
      ["publish-game", "Опубликовать", ""],
      ["cancel-game", "Отменить", ""],
      ["delete-game", "Удалить", ""]
    ])
  ]);
  const table = tableTemplate(sections[section][1], ["Игра", "Дата", "Мастер", "Статус", "Действия"], rows);
  if (section === "schedule") return table;
  return `
    <form class="form-panel game-form">
      <label>Название<input name="title" type="text" value="Новая игра" /></label>
      <label>Система<input name="gameSystem" type="text" value="D&D 5e" /></label>
      <label>Уровень<input name="experienceLevel" type="text" value="newcomer-friendly" /></label>
      <label>Старт<input name="startsAt" type="datetime-local" value="${defaultGameStart()}" /></label>
      <label>Мин. игроков<input name="minPlayers" type="number" min="1" value="3" /></label>
      <label>Макс. игроков<input name="maxPlayers" type="number" min="1" value="5" /></label>
      <label>Длительность<input name="durationMinutes" type="number" min="30" step="30" value="180" /></label>
      <label>Цена<input name="price" type="number" min="0" step="0.01" value="45.00" /></label>
      <label>Master public ID<input name="masterPublicId" type="text" value="usr_mock_master" /></label>
      <label>Описание<textarea name="description" rows="4">Камерная игра для тестового расписания Taverna Control.</textarea></label>
      <label>Staff notes<textarea name="staffNotes" rows="3">Создано из isolated frontend preview.</textarea></label>
      <button type="button" data-action="create-game" title="Создать игру в isolated control backend">Создать игру</button>
    </form>
    ${table}
  `;
}

function genericTemplate(section) {
  const records = recordsFromPayload(state.remote[section]);
  const rows = records.length ? records.map((record) => [
    record.publicId,
    record.title,
    record.status,
    record.updatedAt ? formatDateTime(record.updatedAt) : "read-only",
    actionButtons([
      ["publish-record", "Publish", record.publicId, section],
      ["delete-record", "Archive", record.publicId, section]
    ])
  ]) : [
    [`${section}-001`, "Mock запись", "draft", "edit, publish, soft-delete", actionButtons([["publish-record", "Publish", "", section], ["delete-record", "Archive", "", section]])],
    [`${section}-002`, "Контракт будущего API", "ready", "read-only", actionButtons([["publish-record", "Publish", "", section], ["delete-record", "Archive", "", section]])]
  ];
  return `
    <form class="form-panel">
      <input name="section" type="hidden" value="${escapeHtml(section)}" />
      <label>Название<input name="title" type="text" value="Новая запись ${escapeHtml(sections[section][0])}" /></label>
      <label>Payload<textarea name="payload" rows="4">{ "source": "control-ui", "draft": true }</textarea></label>
      <button type="button" data-action="create-record" title="Создать запись в isolated control backend">Создать запись</button>
    </form>
    ${tableTemplate(sections[section][1], ["ID", "Название", "Статус", "Обновлено", "Действия"], rows.map(([id, title, status, updatedAt, actions]) => [
      id,
      title,
      status,
      updatedAt,
      actions
    ]))}
  `;
}

function tableTemplate(title, headers, rows) {
  const key = tableKey(title);
  const prefs = state.tablePrefs[key] || {};
  const selectedStatus = prefs.status || "all";
  const query = (prefs.query || "").toLowerCase();
  const sort = prefs.sort || "none";
  const pageSize = 8;
  const filtered = rows
    .filter((row) => !query || row.map(cellText).join(" ").toLowerCase().includes(query))
    .filter((row) => selectedStatus === "all" || row.map(cellText).some((cell) => cell.toLowerCase() === selectedStatus));
  const sorted = [...filtered].sort((a, b) => {
    if (sort === "first-asc") return cellText(a[0]).localeCompare(cellText(b[0]), "ru");
    if (sort === "first-desc") return cellText(b[0]).localeCompare(cellText(a[0]), "ru");
    return 0;
  });
  const maxPage = Math.max(1, Math.ceil(sorted.length / pageSize));
  const page = Math.min(Math.max(Number(prefs.page || 1), 1), maxPage);
  const pagedRows = sorted.slice((page - 1) * pageSize, page * pageSize);
  return `
    <section class="table-block ${prefs.columns === "compact" ? "compact" : ""}" data-table-key="${escapeHtml(key)}">
      <div class="table-head">
        <h2>${escapeHtml(title)}</h2>
        <div class="table-tools">
          <input data-table-field="query" type="search" placeholder="Поиск" aria-label="Поиск в таблице" value="${escapeHtml(prefs.query || "")}" />
          <select data-table-field="status" aria-label="Фильтр статуса">
            <option value="all" ${selectedStatus === "all" ? "selected" : ""}>Все статусы</option>
            <option value="draft" ${selectedStatus === "draft" ? "selected" : ""}>draft</option>
            <option value="published" ${selectedStatus === "published" ? "selected" : ""}>published</option>
            <option value="archived" ${selectedStatus === "archived" ? "selected" : ""}>archived</option>
          </select>
          <select data-table-field="sort" aria-label="Сортировка">
            <option value="none" ${sort === "none" ? "selected" : ""}>Без сортировки</option>
            <option value="first-asc" ${sort === "first-asc" ? "selected" : ""}>A-Z</option>
            <option value="first-desc" ${sort === "first-desc" ? "selected" : ""}>Z-A</option>
          </select>
          <button data-table-columns="toggle" title="Колонки">Колонки</button>
          <button title="Экспорт">Export</button>
        </div>
      </div>
      <div class="table-scroll">
        <table>
          <thead><tr><th><input type="checkbox" aria-label="Выбрать все строки" /></th>${headers.map((head) => `<th>${escapeHtml(head)}</th>`).join("")}</tr></thead>
          <tbody>${pagedRows.map((row, index) => `<tr><td><input type="checkbox" aria-label="Выбрать строку ${index + 1}" /></td>${row.map((cell) => `<td>${cellTemplate(cell)}</td>`).join("")}</tr>`).join("")}</tbody>
        </table>
      </div>
      <div class="table-foot">
        <button data-danger="archive" title="Массовое архивирование требует подтверждения">Архивировать выбранные</button>
        <span>Страница <b data-page-output>${page}</b> / ${maxPage}</span>
        <button data-page-action="prev" title="Предыдущая страница" ${page <= 1 ? "disabled" : ""}>Назад</button>
        <button data-page-action="next" title="Следующая страница" ${page >= maxPage ? "disabled" : ""}>Вперёд</button>
      </div>
    </section>
  `;
}

function toRows(payload, fields, fallback) {
  const records = recordsFromPayload(payload);
  if (!Array.isArray(records) || records.length === 0) return fallback;
  return records.map((record) => fields.map((field) => {
    const value = record?.[field];
    if (Array.isArray(value)) return value.join(", ");
    if (value && typeof value === "object") return JSON.stringify(value);
    return value ?? "";
  }));
}

function recordsFromPayload(payload) {
  return Array.isArray(payload) ? payload : payload?.content || payload?.items || [];
}

function cellTemplate(cell) {
  return cell && typeof cell === "object" && "safeHtml" in cell ? cell.safeHtml : escapeHtml(cell);
}

function cellText(cell) {
  if (cell && typeof cell === "object" && "safeHtml" in cell) return "";
  return String(cell ?? "");
}

function tableKey(title) {
  return `${state.section}:${title}`.replace(/\s+/g, "-").toLowerCase();
}

function loadTablePrefs() {
  try {
    return JSON.parse(localStorage.getItem("control-table-prefs") || "{}");
  } catch {
    return {};
  }
}

function saveTablePrefs() {
  localStorage.setItem("control-table-prefs", JSON.stringify(state.tablePrefs));
}

function updateTablePref(table, key, value) {
  state.tablePrefs[table] = { ...(state.tablePrefs[table] || {}), [key]: value, page: key === "page" ? value : 1 };
  saveTablePrefs();
  render();
}

function bindTableControls() {
  document.querySelectorAll("[data-table-field]").forEach((control) => {
    const table = control.closest("[data-table-key]")?.dataset.tableKey;
    if (!table) return;
    const eventName = control.tagName === "INPUT" ? "input" : "change";
    control.addEventListener(eventName, () => updateTablePref(table, control.dataset.tableField, control.value));
  });
  document.querySelectorAll("[data-table-columns]").forEach((button) => {
    button.addEventListener("click", () => {
      const table = button.closest("[data-table-key]")?.dataset.tableKey;
      if (!table) return;
      updateTablePref(table, "columns", state.tablePrefs[table]?.columns === "compact" ? "all" : "compact");
    });
  });
}

function renderQrCanvases() {
  document.querySelectorAll("canvas.totp-qr[data-qr]").forEach((canvas) => drawScannableQr(canvas, canvas.dataset.qr));
}

async function drawScannableQr(canvas, text) {
  if (window.QRCode?.toCanvas) {
    try {
      await window.QRCode.toCanvas(canvas, text, {
        errorCorrectionLevel: "M",
        margin: 1,
        width: canvas.width,
        color: {
          dark: "#17120e",
          light: "#fff8e9"
        }
      });
      return;
    } catch {
      drawQrPreview(canvas, text);
      return;
    }
  }
  drawQrPreview(canvas, text);
}

function drawQrPreview(canvas, text) {
  const ctx = canvas.getContext("2d");
  const modules = 29;
  const cell = Math.floor(canvas.width / modules);
  ctx.fillStyle = "#fff8e9";
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  const hash = hashText(text);
  drawFinder(ctx, 1, 1, cell);
  drawFinder(ctx, modules - 8, 1, cell);
  drawFinder(ctx, 1, modules - 8, cell);
  for (let y = 0; y < modules; y += 1) {
    for (let x = 0; x < modules; x += 1) {
      if (isFinderArea(x, y, modules)) continue;
      const bit = ((hash[(x + y * modules) % hash.length] + x * 17 + y * 31) % 7) < 3;
      if (bit) drawModule(ctx, x, y, cell);
    }
  }
}

function drawFinder(ctx, x, y, cell) {
  for (let row = 0; row < 7; row += 1) {
    for (let col = 0; col < 7; col += 1) {
      const edge = row === 0 || col === 0 || row === 6 || col === 6;
      const center = row >= 2 && row <= 4 && col >= 2 && col <= 4;
      if (edge || center) drawModule(ctx, x + col, y + row, cell);
    }
  }
}

function drawModule(ctx, x, y, cell) {
  ctx.fillStyle = "#17120e";
  ctx.fillRect(x * cell + 2, y * cell + 2, cell, cell);
}

function isFinderArea(x, y, modules) {
  return (x <= 8 && y <= 8) || (x >= modules - 9 && y <= 8) || (x <= 8 && y >= modules - 9);
}

function hashText(text) {
  const bytes = new TextEncoder().encode(text || "");
  let a = 2166136261;
  const out = [];
  bytes.forEach((byte) => {
    a ^= byte;
    a = Math.imul(a, 16777619);
    out.push(a & 255, (a >> 8) & 255, (a >> 16) & 255, (a >> 24) & 255);
  });
  return out.length ? out : [0];
}

function actionButtons(actions) {
  return {
    safeHtml: `<div class="inline-actions">${actions.map(([action, label, id, section]) => `
      <button type="button" data-action="${escapeHtml(action)}" data-id="${escapeHtml(id)}" ${section ? `data-section-key="${escapeHtml(section)}"` : ""} ${id ? "" : "disabled"}>${escapeHtml(label)}</button>
    `).join("")}</div>`
  };
}

function formatDateTime(value) {
  if (!value || typeof value !== "string" || Number.isNaN(Date.parse(value))) return value || "";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function defaultGameStart() {
  const start = new Date();
  start.setDate(start.getDate() + 7);
  start.setHours(19, 0, 0, 0);
  return start.toISOString().slice(0, 16);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function sessionLabel() {
  if (!state.account) return "Session: not authenticated";
  const roles = Array.isArray(state.account.roles) ? state.account.roles.join(", ") : state.account.roles;
  return `${state.account.displayName || state.account.email} / ${roles || "no roles"}`;
}

function formJson(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function gamePayload(body) {
  return {
    title: body.title,
    description: body.description,
    gameSystem: body.gameSystem,
    experienceLevel: body.experienceLevel,
    startsAt: new Date(body.startsAt).toISOString(),
    durationMinutes: Number(body.durationMinutes),
    minPlayers: Number(body.minPlayers),
    maxPlayers: Number(body.maxPlayers),
    price: Number(body.price),
    masterPublicId: body.masterPublicId,
    staffNotes: body.staffNotes
  };
}

function xsrfToken() {
  return document.cookie
    .split("; ")
    .find((part) => part.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
}

async function ensureCsrf() {
  await apiGet("/api/v1/auth/csrf");
  return decodeURIComponent(xsrfToken() || "");
}

async function apiPost(path, body, csrf = false) {
  const headers = {
    Accept: "application/json",
    "Content-Type": "application/json"
  };
  if (csrf) headers["X-XSRF-TOKEN"] = await ensureCsrf();
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    credentials: "include",
    headers,
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    const error = new Error(`POST ${path} failed with ${response.status}`);
    error.status = response.status;
    throw error;
  }
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

async function apiDelete(path) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Accept: "application/json",
      "X-XSRF-TOKEN": await ensureCsrf()
    }
  });
  if (!response.ok) {
    const error = new Error(`DELETE ${path} failed with ${response.status}`);
    error.status = response.status;
    throw error;
  }
}

async function runAction(action, form, sourceElement = null) {
  const body = form ? formJson(form) : {};
  state.actionStatus = `${action}: sending...`;
  render();
  try {
    if (action === "login") {
      state.account = await apiPost("/api/v1/auth/login", body);
      state.actionStatus = `Logged in as ${state.account.email}`;
    } else if (action === "me") {
      state.account = await apiGet("/api/v1/auth/me");
      state.actionStatus = `Session active for ${state.account.email}`;
    } else if (action === "password-reset") {
      const result = await apiPost("/api/v1/auth/password-reset", body);
      state.actionStatus = result.devOnlyToken ? `Reset token issued: ${result.devOnlyToken}` : "Password reset response accepted";
    } else if (action === "2fa-setup") {
      state.twoFactorSetup = await apiPost("/api/v1/account/2fa/setup", {}, true);
      state.actionStatus = "2FA setup QR generated";
    } else if (action === "2fa-confirm") {
      const result = await apiPost("/api/v1/account/2fa/confirm", body, true);
      state.actionStatus = `2FA enabled. Backup codes: ${(result.backupCodes || []).join(", ")}`;
      state.twoFactorSetup = null;
    } else if (action === "2fa-disable") {
      await apiPost("/api/v1/account/2fa/disable", body, true);
      state.actionStatus = "2FA disabled";
    } else if (action === "sessions-refresh") {
      state.remote.securitySessions = await apiGet("/api/v1/account/sessions");
      state.actionStatus = "Sessions refreshed";
    } else if (action === "sessions-revoke") {
      await apiPost("/api/v1/account/sessions/revoke-all", {}, true);
      state.account = null;
      state.remote.securitySessions = [];
      state.actionStatus = "All sessions revoked";
    } else if (action === "invite-user") {
      const invitation = await apiPost("/api/v1/admin/users/invitations", body, true);
      state.actionStatus = `Invitation created: ${invitation.oneTimeToken || invitation.id}`;
      await loadSectionData("users");
    } else if (action === "create-record") {
      const { section, title, payload } = body;
      const record = await apiPost(`/api/v1/admin/data/${section}`, { title, payload }, true);
      state.actionStatus = `Record saved: ${record.publicId || record.title}`;
      await loadSectionData(section);
    } else if (action === "create-game") {
      const game = await apiPost("/api/v1/admin/games", gamePayload(body), true);
      state.actionStatus = `Game created: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "publish-game") {
      const game = await apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/publish`, {}, true);
      state.actionStatus = `Game published: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "cancel-game") {
      const game = await apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/cancel`, {}, true);
      state.actionStatus = `Game cancelled: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "delete-game") {
      await apiDelete(`/api/v1/admin/games/${sourceElement?.dataset.id}`);
      state.actionStatus = "Game archived";
      await loadSectionData("games");
    } else if (action === "publish-record") {
      const section = sourceElement?.dataset.sectionKey;
      const record = await apiPost(`/api/v1/admin/data/${section}/${sourceElement?.dataset.id}/publish`, {}, true);
      state.actionStatus = `Record published: ${record.publicId || record.title}`;
      await loadSectionData(section);
    } else if (action === "delete-record") {
      const section = sourceElement?.dataset.sectionKey;
      await apiDelete(`/api/v1/admin/data/${section}/${sourceElement?.dataset.id}`);
      state.actionStatus = "Record archived";
      await loadSectionData(section);
    } else if (action === "project-launch") {
      const projectCode = sourceElement?.dataset.project;
      const result = await apiPost(`/api/v1/admin/projects/${projectCode}/launch`, {}, true);
      state.actionStatus = `${result.code}: ${result.status}`;
      await loadSectionData("projects");
    } else if (action === "backup-create") {
      const backup = await apiPost("/api/v1/admin/backups", {}, true);
      state.actionStatus = `Backup created: ${backup.publicId || backup.status}`;
      await loadSectionData("backups");
    } else if (action === "backup-restore") {
      const result = await apiPost("/api/v1/admin/backups/restore", {}, true);
      state.actionStatus = result.reason || "Restore is disabled";
    }
    state.backend = "online";
  } catch (error) {
    state.backend = error.status ? "online" : "offline";
    state.actionStatus = actionErrorMessage(action, error);
  } finally {
    render();
  }
}

function actionErrorMessage(action, error) {
  if (!error.status) return `${action}: backend is not reachable on ${API_BASE}`;
  if (error.status === 401 || error.status === 403) return `${action}: backend answered ${error.status}; login or permission required`;
  return `${action}: backend answered ${error.status}`;
}

function updateAutosave() {
  const label = document.querySelector("#autosave");
  if (label) label.textContent = state.autosave === "saved" ? "Сохранено" : "Сохраняю...";
}

async function checkBackend() {
  try {
    const response = await fetch(`${API_BASE}/actuator/health/liveness`, { credentials: "include" });
    state.backend = response.ok ? "online" : "offline";
  } catch {
    state.backend = "offline";
  }
  updateBackendStatus();
}

function updateBackendStatus() {
  const statusLine = document.querySelector(".status-line");
  if (statusLine) {
    const dataSource = sectionDataSource(state.section);
    statusLine.innerHTML = `<span><span class="dot ${state.backend === "online" ? "ok" : ""}"></span>Backend: ${state.backend}</span><span class="source-badge ${dataSource}">Data: ${dataSource}</span>`;
  }
}

function sectionDataSource(section) {
  if (state.loadingSection === section) return "loading";
  return state.remote[remoteKey(section)] ? "backend" : "mock";
}

function remoteKey(section) {
  if (section === "security") return "securitySessions";
  return section === "files" ? "filesStorage" : section;
}

async function apiGet(path) {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    headers: { Accept: "application/json" }
  });
  if (!response.ok) {
    const error = new Error(`GET ${path} failed with ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

function sectionEndpoint(section) {
  if (section === "overview") return "/api/v1/admin/dashboard";
  if (section === "projects") return "/api/v1/admin/projects";
  if (section === "users") return "/api/v1/admin/users";
  if (section === "files") return "/api/v1/admin/files/storage";
  if (section === "backups") return "/api/v1/admin/backups";
  if (section === "security") return "/api/v1/account/sessions";
  if (section === "settings") return "/api/v1/admin/settings";
  if (section === "tech") return "/api/v1/admin/integration/status";
  if (section === "audit") return "/api/v1/admin/audit?page=0";
  if (section === "games") return "/api/v1/admin/games?page=0&size=20";
  if (section === "schedule") {
    const from = new Date();
    const to = new Date(from);
    to.setDate(to.getDate() + 45);
    return `/api/v1/admin/schedule?from=${encodeURIComponent(from.toISOString())}&to=${encodeURIComponent(to.toISOString())}`;
  }
  if (genericDataSections.has(section)) return `/api/v1/admin/data/${section}?page=0&size=20`;
  return null;
}

async function loadSectionData(section) {
  const endpoint = sectionEndpoint(section);
  if (!endpoint) return;
  state.loadingSection = section;
  updateBackendStatus();
  try {
    const payload = await apiGet(endpoint);
    state.remote[remoteKey(section)] = payload;
    state.backend = "online";
  } catch (error) {
    delete state.remote[remoteKey(section)];
    state.backend = error.status ? "online" : "offline";
  } finally {
    if (state.loadingSection === section) state.loadingSection = null;
    if (state.section === section) render();
  }
}

render();
checkBackend();
loadSectionData(state.section);
