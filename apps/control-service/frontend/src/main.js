import "./styles.css";

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
  settings: ["Настройки", "Флаги интеграций"],
  tech: ["Состояние", "Backend health"]
};

const state = {
  role: "OWNER",
  section: "overview",
  backend: "unknown",
  autosave: "saved"
};

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
          </div>
          <div class="status-line" aria-live="polite">
            <span class="dot ${state.backend === "online" ? "ok" : ""}"></span>
            Backend: ${state.backend}
          </div>
        </header>
        <section class="content">
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
  });
  document.querySelectorAll("[data-section]").forEach((button) => {
    button.addEventListener("click", () => {
      state.section = button.dataset.section;
      render();
    });
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
  checkBackend();
}

function sectionTemplate(section) {
  if (section === "overview") return overviewTemplate();
  if (section === "projects") return projectsTemplate();
  if (section === "users") return usersTemplate();
  if (section === "stories") return storiesTemplate();
  if (section === "tech") return techTemplate();
  if (section === "audit") return tableTemplate("Последние действия", ["Кто", "Операция", "Объект", "Когда"], mock.audit);
  if (section === "backups") return backupsTemplate();
  return genericTemplate(section);
}

function overviewTemplate() {
  return `
    <div class="metric-grid">
      ${mock.metrics.map(([label, value, hint]) => `<article class="metric"><span>${label}</span><strong>${value}</strong><small>${hint}</small></article>`).join("")}
    </div>
    ${tableTemplate("Ближайшие игры", ["Игра", "Дата", "Мастер", "Статус"], mock.games)}
    ${tableTemplate("Последние операции рейтинга", ["Игрок", "Операция", "Очки", "Статус"], [
      ["Мария", "game_result", "+12", "applied"],
      ["Илья", "correction", "-2", "needs review"]
    ])}
  `;
}

function projectsTemplate() {
  return `
    <div class="project-grid">
      ${mock.projects.map(([name, stack, path, status]) => `
        <article class="project">
          <h3>${name}</h3>
          <p>${stack}</p>
          <code>${path}</code>
          <span>${status}</span>
          <button title="Создать запись mock-launch без запуска программы">Mock launch</button>
        </article>
      `).join("")}
    </div>
    <p class="note">Реальный запуск требует Desktop Agent, service auth и allowlist. Frontend не передаёт путь запуска.</p>
  `;
}

function usersTemplate() {
  return `
    <form class="form-panel">
      <label>Email<input type="email" value="master@example.test" /></label>
      <label>Имя<input type="text" value="Новый мастер" /></label>
      <label>Роль<select><option>MASTER</option><option>CONTENT_MANAGER</option><option>RATING_MANAGER</option></select></label>
      <button type="button" title="Создать одноразовое приглашение">Создать приглашение</button>
    </form>
    ${tableTemplate("Роли", ["Роль", "Смысл", "2FA"], [
      ["OWNER", "Полный доступ, последнего удалить нельзя", "required"],
      ["SUPERADMIN", "Администрирование системы", "required"],
      ["MASTER", "Свои игры, расписание, программы", "optional"]
    ])}
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
  return `
    <div class="actions">
      <button title="Создать manifest backup в локальном storage">Создать backup</button>
      <button class="danger" title="Восстановление требует отдельного подтверждения">Восстановление выключено</button>
    </div>
    ${tableTemplate("Backup jobs", ["ID", "Статус", "Проверка", "Restore"], [
      ["bkp_mock_01", "COMPLETED", "checksum ok", "disabled"],
      ["bkp_mock_02", "PLANNED", "not started", "disabled"]
    ])}
  `;
}

function techTemplate() {
  return `
    <div class="tech">
      <p>Backend health: <strong>${state.backend}</strong></p>
      <p>PUBLIC_REGISTRATION_ENABLED: <strong>false</strong></p>
      <p>Main site integration: <strong>false</strong></p>
      <p>Telegram integration: <strong>false</strong></p>
      <p>Desktop Agent: <strong>false</strong></p>
    </div>
  `;
}

function genericTemplate(section) {
  return tableTemplate(sections[section][1], ["ID", "Название", "Статус", "Действия"], [
    [`${section}-001`, "Mock запись", "draft", "edit, publish, soft-delete"],
    [`${section}-002`, "Контракт будущего API", "ready", "read-only"]
  ]);
}

function tableTemplate(title, headers, rows) {
  return `
    <section class="table-block">
      <div class="table-head">
        <h2>${title}</h2>
        <input type="search" placeholder="Поиск" aria-label="Поиск в таблице" />
      </div>
      <div class="table-scroll">
        <table>
          <thead><tr>${headers.map((head) => `<th>${head}</th>`).join("")}</tr></thead>
          <tbody>${rows.map((row) => `<tr>${row.map((cell) => `<td>${cell}</td>`).join("")}</tr>`).join("")}</tbody>
        </table>
      </div>
    </section>
  `;
}

function updateAutosave() {
  const label = document.querySelector("#autosave");
  if (label) label.textContent = state.autosave === "saved" ? "Сохранено" : "Сохраняю...";
}

async function checkBackend() {
  try {
    const response = await fetch("http://localhost:4190/actuator/health/liveness", { credentials: "include" });
    state.backend = response.ok ? "online" : "offline";
  } catch {
    state.backend = "offline";
  }
  const statusLine = document.querySelector(".status-line");
  if (statusLine) {
    statusLine.innerHTML = `<span class="dot ${state.backend === "online" ? "ok" : ""}"></span>Backend: ${state.backend}`;
  }
}

render();
