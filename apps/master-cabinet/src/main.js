const API_BASE = window.CONTROL_API_BASE ?? "";
const ACCESS_STORAGE_KEY = "tavernaDiaryAccess";
const TABLE_PREFS_KEY = "control-table-prefs";
const NOTIFICATIONS_SEEN_KEY = "tavernaCabinetLastSeenAction";

const roles = {
  HATTER: ["overview", "games", "applications", "gallery", "rating", "masters", "profile"],
  MASTER: ["overview", "games", "applications", "gallery", "rating", "profile"],
  PLAYER: ["player"]
};

const sections = {
  overview: ["Главная", "Моя таверна"],
  games: ["Игры", "Расписание, публикация и правки"],
  applications: ["Заявки", "Обращения с сайта"],
  gallery: ["Галерея", "Фотографии и истории"],
  rating: ["Рейтинг", "Игроки и очки"],
  masters: ["Доступ", "Мастера и заявки"],
  profile: ["Профиль", "Данные мастерского аккаунта"],
  player: ["Моя таверна", "Профиль игрока и будущие записи"]
};

const app = document.querySelector("#app");

const state = {
  account: loadAccessAccount(),
  role: normalizeRole(loadAccessAccount()?.systemRole || loadAccessAccount()?.role),
  section: initialSection(normalizeRole(loadAccessAccount()?.systemRole || loadAccessAccount()?.role)),
  loadingSection: null,
  selectedGameId: "",
  selectedProfileId: "",
  selectedGalleryId: "",
  selectedRatingId: "",
  profileEditing: false,
  galleryModal: null,
  tablePrefs: loadTablePrefs(),
  lastSeenAction: localStorage.getItem(NOTIFICATIONS_SEEN_KEY) || "",
  notice: "",
  remote: {}
};

function loadAccessAccount() {
  try {
    return JSON.parse(localStorage.getItem(ACCESS_STORAGE_KEY) || "null");
  } catch {
    return null;
  }
}

function normalizeRole(role) {
  const value = String(role || "").toLowerCase();
  if (value === "player") return "PLAYER";
  return value === "admin" || value === "hatter" ? "HATTER" : "MASTER";
}

function initialSection(role) {
  const hash = window.location.hash.replace("#", "").trim();
  return roles[role]?.includes(hash) ? hash : "overview";
}

function render() {
  if (!state.account?.accessGranted) {
    renderGate();
    return;
  }

  const visible = roles[state.role] || roles.PLAYER;
  app.innerHTML = `
    <div class="cabinet-shell">
      <aside class="cabinet-sidebar" aria-label="Разделы кабинета">
        <div class="cabinet-section-title">
          <span>Раздел сайта</span>
          <b>Моя таверна</b>
        </div>
        ${profileSwitchTemplate()}
        <nav class="cabinet-nav">
          ${visible.map((key) => `
            <button class="nav-item ${state.section === key ? "is-active" : ""}" type="button" data-section="${key}">
              <span>${sections[key][0]}</span>
              <small>${sections[key][1]}</small>
            </button>
          `).join("")}
        </nav>
        ${notificationsTemplate()}
      </aside>

      <main class="cabinet-workspace">
        <header class="cabinet-topbar">
          <div>
            <p class="eyebrow">${state.role === "HATTER" ? "Полный доступ" : state.role === "PLAYER" ? "Игровой доступ" : "Мастерский доступ"}</p>
            <h1>${state.section === "overview" ? "Моя таверна" : sections[state.section][0]}</h1>
            <p>${sections[state.section][1]}</p>
          </div>
          <div class="topbar-actions">
            <button type="button" data-action="refresh">Обновить</button>
            <button type="button" data-action="logout">Выйти</button>
          </div>
        </header>

        <section class="cabinet-content">
          ${state.notice ? `<div class="cabinet-notice" role="status">${escapeHtml(state.notice)}</div>` : ""}
          ${sectionTemplate(state.section)}
        </section>
      </main>
    </div>
    ${modalTemplate()}
  `;

  bindEvents();
}

function renderGate() {
  app.innerHTML = `
    <main class="cabinet-gate">
      <section class="cabinet-gate__card">
        <img src="./hatter-mark.png" alt="" />
        <p class="eyebrow">Скрытая стойка Таверны</p>
        <h1>Моя таверна закрыта</h1>
        <p>Дневник можно читать свободно, а рабочий кабинет открывается только подтверждённым мастерам.</p>
        <button type="button" data-open-diary-login>Открыть вход</button>
      </section>
    </main>
  `;
  document.querySelector("[data-open-diary-login]")?.addEventListener("click", () => {
    window.location.assign(new URL("/hatter-diary.html", window.location.origin).href);
  });
}

function profileSwitchTemplate() {
  if (state.role === "PLAYER") {
    return `
      <section class="active-master-card">
        <img src="./hatter-mark.png" alt="" />
        <div>
          <span>Игрок Таверны</span>
          <strong>${escapeHtml(state.account?.displayName || "Гость у стола")}</strong>
          <small>личный профиль</small>
        </div>
      </section>
    `;
  }
  const mode = state.account?.profileMode || (state.role === "HATTER" ? "hatter" : "master");
  const canSwitch = Boolean(state.account?.canSwitchProfile);
  return `
    <section class="active-master-card">
      <img src="${mode === "hatter" ? "./hatter-cabinet-bg.jpg" : "/assets/images/masters/alexander.jpeg"}" alt="" />
      <div>
        <span>${mode === "hatter" ? "Шляпник" : "Мастер Александр"}</span>
        <strong>${escapeHtml(state.account?.displayName || "Мастер Таверны")}</strong>
        <small>${sessionLabel()}</small>
      </div>
      ${canSwitch ? `
        <div class="mode-switch" aria-label="Режим кабинета">
          <button type="button" class="${mode === "hatter" ? "is-active" : ""}" data-action="switch-mode" data-mode="hatter">Шляпник</button>
          <button type="button" class="${mode === "master" ? "is-active" : ""}" data-action="switch-mode" data-mode="master">Александр</button>
        </div>
      ` : ""}
    </section>
  `;
}

function notificationsTemplate() {
  const actions = recentActions().slice(0, 4);
  const unread = unreadActions().length;
  return `
    <section class="recent-actions" data-notifications>
      <div class="recent-actions__head">
        <span>Последние действия</span>
        ${unread ? `<b aria-label="Новые действия">${unread}</b>` : ""}
      </div>
      ${actions.length ? actions.map((action) => `
        <button type="button" class="recent-action ${isUnread(action) ? "is-unread" : ""}" data-action="mark-notifications-read">
          <span>${escapeHtml(humanAction(action))}</span>
          <small>${escapeHtml(action.entityType || action.entity || "Таверна")} · ${formatDateTime(action.createdAt)}</small>
        </button>
      `).join("") : `<p>Пока нет новых записей.</p>`}
    </section>
  `;
}

function sectionTemplate(section) {
  if (section === "player") return playerTemplate();
  if (section === "overview") return overviewTemplate();
  if (section === "games") return gamesTemplate();
  if (section === "applications") return applicationsTemplate();
  if (section === "gallery") return adminGalleryTemplate();
  if (section === "rating") return adminRatingTemplate();
  if (section === "masters") return accessRequestsTemplate();
  if (section === "profile") return adminProfileTemplate();
  return `<p class="empty-state">Раздел пока готовится.</p>`;
}

function playerTemplate() {
  return `
    <section class="overview-hero">
      <div class="overview-portrait">
        <img src="./hatter-mark.png" alt="" />
      </div>
      <div>
        <p class="eyebrow">Игрок Таверны</p>
        <h2>${escapeHtml(state.account?.displayName || "Гость у стола")}</h2>
        <p>Здесь будет личная страница игрока: профиль, записи на игры, рейтинг и история приключений. Основа аккаунта уже работает через серверную сессию.</p>
      </div>
    </section>
    <div class="metric-grid">
      ${metricCard("E-mail", state.account?.email || "не указан", "для входа")}
      ${metricCard("Telegram", state.account?.telegramUsername || "можно добавить позже", "контакт")}
      ${metricCard("Записи", "скоро", "игры")}
      ${metricCard("Рейтинг", "скоро", "очки")}
    </div>
    <div class="card-list">
      <article class="data-card">
        <div>
          <p class="eyebrow">Следующий шаг</p>
          <h3>Выберите игру в афише</h3>
          <p>Пока личные записи игрока готовятся, расписание доступно в общей афише Таверны.</p>
        </div>
        <div class="inline-actions">
          <a class="ghost-link" href="/#games">Открыть афишу</a>
          <a class="ghost-link" href="/rating.html">Посмотреть рейтинг</a>
        </div>
      </article>
    </div>
  `;
}

function overviewTemplate() {
  const snapshot = state.remote.overview;
  const metrics = snapshot?.metrics || [];
  const games = filterOwnGames(snapshot?.upcomingGames || []);
  const requests = recordsFromPayload(state.remote.applications);
  return `
    <section class="overview-hero">
      <div class="overview-portrait">
        <img src="${state.account?.profileMode === "master" ? "/assets/images/masters/alexander.jpeg" : "./hatter-mark.png"}" alt="" />
      </div>
      <div>
        <p class="eyebrow">${state.account?.profileMode === "master" ? "За мастерским столом" : "Моя таверна Шляпника"}</p>
        <h2>${escapeHtml(state.account?.displayName || "Мастер Таверны")}</h2>
        <p>${state.role === "HATTER"
          ? "Здесь можно смотреть всю Таверну целиком: игры, заявки, галерею, рейтинг и доступ мастеров."
          : "Здесь собраны ваши игры, заявки, публикации и инструменты для спокойной подготовки сессий."}</p>
      </div>
    </section>
    <div class="metric-grid">
      ${metrics.slice(0, 4).map((metric) => metricCard(metric.label, metric.value, metric.tone)).join("")}
      ${metricCard("Ожидают внимания", requests.filter((item) => item.status === "new").length, "заявки")}
    </div>
    ${cardListTemplate("Ближайшие игры", games, gameCard)}
  `;
}

function gamesTemplate() {
  const records = filterOwnGames(recordsFromPayload(state.remote.games));
  const masters = recordsFromPayload(state.remote.masters);
  return `
    <section class="work-panel">
      <div>
        <p class="eyebrow">Новая запись в расписании</p>
        <h2>Создать игру</h2>
      </div>
      <form class="form-grid" data-create-game-form>
        <label>Название<input name="title" type="text" value="Новая игра" /></label>
        <label>Система<input name="gameSystem" type="text" value="D&D 5e" /></label>
        <label>Уровень<input name="experienceLevel" type="text" value="любой уровень" /></label>
        <label>Возраст<input name="ageRating" type="text" value="12+" /></label>
        <label>Дата и время<input name="startsAt" type="datetime-local" value="${defaultGameStart()}" /></label>
        <label>Длительность, минут<input name="durationMinutes" type="number" min="30" step="30" value="180" /></label>
        <label>Мин. игроков<input name="minPlayers" type="number" min="1" value="3" /></label>
        <label>Макс. игроков<input name="maxPlayers" type="number" min="1" value="5" /></label>
        <label>Стоимость<input name="price" type="number" min="0" step="0.01" value="45.00" /></label>
        <label>Валюта<input name="currency" type="text" value="BYN" /></label>
        ${masterSelectTemplate(masters)}
        <label>Ссылка записи<input name="contactUrl" type="text" value="https://t.me/Taverna_Shlyapnika" /></label>
        <label class="form-wide">Описание<textarea name="description" rows="4">Описание будет добавлено мастером.</textarea></label>
        <label class="form-wide">Заметки для команды<textarea name="staffNotes" rows="3">Создано из кабинета мастера.</textarea></label>
        <button type="button" data-action="create-game">Создать игру</button>
      </form>
    </section>
    ${gameEditorTemplate(records, masters)}
    ${gamesCalendarTemplate(records)}
    ${cardListTemplate("Игры в расписании", records, gameCard)}
  `;
}

function gamesCalendarTemplate(records) {
  const today = new Date();
  const days = Array.from({ length: 35 }, (_, index) => {
    const date = new Date(today);
    date.setDate(today.getDate() + index);
    date.setHours(0, 0, 0, 0);
    return date;
  });
  return `
    <section class="work-panel calendar-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Общая занятость Таверны</p>
          <h2>Календарь игр</h2>
        </div>
        <span class="calendar-hint">нижний огонь — днём, верхний — вечером</span>
      </div>
      <div class="cabinet-calendar" aria-label="Календарь занятости">
        ${days.map((day) => calendarDayTemplate(day, records)).join("")}
      </div>
    </section>
  `;
}

function calendarDayTemplate(day, records) {
  const dayKey = dateTimeLocalValue(day.toISOString()).slice(0, 10);
  const events = records.filter((game) => dateTimeLocalValue(game.startsAt).slice(0, 10) === dayKey);
  const dayEvents = events.filter((game) => new Date(game.startsAt).getHours() < 16);
  const eveningEvents = events.filter((game) => new Date(game.startsAt).getHours() >= 16);
  const title = events.length
    ? events.map((game) => `${game.title || "Игра"} · ${formatDateTime(game.startsAt)} · ${game.masterName || "мастер не указан"} · ${game.status || ""}`).join("\n")
    : "Свободно";
  return `
    <button type="button" class="calendar-day ${events.length ? "is-busy" : ""}" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}">
      <span class="calendar-dot calendar-dot-evening ${eveningEvents.length ? "is-active" : ""}"></span>
      <strong>${day.getDate()}</strong>
      <small>${day.toLocaleDateString("ru-RU", { weekday: "short" })}</small>
      <span class="calendar-dot calendar-dot-day ${dayEvents.length ? "is-active" : ""}"></span>
    </button>
  `;
}

function masterSelectTemplate(masters) {
  if (!masters.length) {
    return `<label>Мастер<input name="masterPublicId" type="text" placeholder="ID мастера из базы" /></label>`;
  }
  return `
    <label>Мастер
      <select name="masterPublicId">
        ${masters.map((master) => `<option value="${escapeHtml(master.publicId)}">${escapeHtml(master.title)}</option>`).join("")}
      </select>
    </label>
  `;
}

function gameEditorTemplate(records, masters) {
  const game = records.find((item) => item.id === state.selectedGameId);
  if (!game) {
    return `
      <section class="work-panel muted-panel">
        <h2>Редактор игры</h2>
        <p>Выберите игру в списке ниже и нажмите «Редактировать». Поля откроются здесь без перехода в Telegram.</p>
      </section>
    `;
  }
  const masterOptions = masters.length
    ? masters.map((master) => `<option value="${escapeHtml(master.publicId)}" ${master.publicId === game.masterPublicId ? "selected" : ""}>${escapeHtml(master.title)}</option>`).join("")
    : `<option value="${escapeHtml(game.masterPublicId || "")}">${escapeHtml(game.masterName || game.masterPublicId || "Мастер")}</option>`;
  return `
    <section class="work-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Редактор игры</p>
          <h2>${escapeHtml(game.title)}</h2>
        </div>
        <span class="status-pill">${escapeHtml(game.status)}</span>
      </div>
      <form class="form-grid" data-game-editor-form>
        <input type="hidden" name="id" value="${escapeHtml(game.id)}" />
        <label>Название<input name="title" type="text" value="${escapeHtml(game.title)}" /></label>
        <label>Система<input name="gameSystem" type="text" value="${escapeHtml(game.gameSystem || "D&D 5e")}" /></label>
        <label>Дата и время<input name="startsAt" type="datetime-local" value="${dateTimeLocalValue(game.startsAt)}" /></label>
        <label>Длительность<input name="durationMinutes" type="number" min="30" step="30" value="${escapeHtml(game.durationMinutes || 180)}" /></label>
        <label>Мин. игроков<input name="minPlayers" type="number" min="1" value="${escapeHtml(game.minPlayers || 1)}" /></label>
        <label>Макс. игроков<input name="maxPlayers" type="number" min="1" value="${escapeHtml(game.maxPlayers || 5)}" /></label>
        <label>Стоимость<input name="price" type="number" min="0" step="0.01" value="${escapeHtml(game.price || 0)}" /></label>
        <label>Валюта<input name="currency" type="text" value="${escapeHtml(game.currency || "BYN")}" /></label>
        <label>Мастер<select name="masterPublicId">${masterOptions}</select></label>
        <label>Ссылка записи<input name="contactUrl" type="text" value="${escapeHtml(game.contactUrl || "")}" /></label>
        <label class="form-wide">Описание<textarea name="description" rows="4">${escapeHtml(game.description || "")}</textarea></label>
        <div class="form-actions">
          <button type="button" data-action="save-game">Сохранить</button>
          <button type="button" data-action="clear-game-editor">Закрыть</button>
        </div>
      </form>
    </section>
  `;
}

function applicationsTemplate() {
  const records = recordsFromPayload(state.remote.applications);
  return cardListTemplate("Заявки с сайта", records, (record) => `
    <article class="data-card">
      <div>
        <p class="eyebrow">${escapeHtml(record.publicId)}</p>
        <h3>${escapeHtml(record.title || "Заявка")}</h3>
        <p>${escapeHtml(record.status || "new")} · ${formatDateTime(record.updatedAt)}</p>
      </div>
      <div class="inline-actions">
        <button type="button" data-action="contact-service-request" data-id="${escapeHtml(record.publicId)}">В работу</button>
        <button type="button" data-action="close-service-request" data-id="${escapeHtml(record.publicId)}">Закрыть</button>
      </div>
    </article>
  `);
}

function galleryTemplate() {
  const posts = recordsFromPayload(state.remote.gallery);
  return cardListTemplate("Публикации галереи", posts, (post) => `
    <article class="data-card">
      <div>
        <p class="eyebrow">${escapeHtml(galleryTypeLabel(post.type))} · ${escapeHtml(galleryCategoryLabel(post.category))}</p>
        <h3>${escapeHtml(post.title || post.publicId)}</h3>
        <p>${formatDate(post.eventDate || post.publishedAt || post.createdAt)} · ${escapeHtml(post.mediaCount || 0)} фото · ${escapeHtml(post.status)}${post.visible ? "" : " / скрыто"}</p>
      </div>
      <div class="inline-actions">
        <button type="button" data-action="publish-gallery-post" data-id="${escapeHtml(post.publicId)}">Опубликовать</button>
        <button type="button" data-action="hide-gallery-post" data-id="${escapeHtml(post.publicId)}">Скрыть</button>
        <button type="button" data-action="delete-gallery-post" data-id="${escapeHtml(post.publicId)}">Удалить</button>
      </div>
    </article>
  `);
}

function ratingTemplate() {
  const players = recordsFromPayload(state.remote.rating);
  const summary = players.reduce((acc, player) => {
    acc.games += Number(player.gamesPlayed || 0);
    acc.points += Number(player.totalPoints || 0);
    acc.inspiration += Number(player.inspirationCount || 0);
    return acc;
  }, { games: 0, points: 0, inspiration: 0 });
  return `
    <div class="metric-grid rating-summary">
      ${metricCard("Игроки", players.length, "в рейтинге")}
      ${metricCard("Игры", summary.games, "учтены")}
      ${metricCard("Очки", summary.points, "общая сумма")}
      ${metricCard("Вдохновение", summary.inspiration, "всего")}
    </div>
    ${cardListTemplate("Список игроков", players, ratingCard)}
  `;
}

function mastersTemplate() {
  const masters = recordsFromPayload(state.remote.masters);
  return cardListTemplate("Мастера и доступы", masters, (master) => `
    <article class="data-card">
      <div>
        <p class="eyebrow">${escapeHtml(master.publicId)}</p>
        <h3>${escapeHtml(master.title || "Мастер")}</h3>
        <p>${escapeHtml(master.status || "active")} · ${formatDateTime(master.updatedAt)}</p>
      </div>
      <div class="inline-actions">
        <button type="button" data-action="activate-master" data-id="${escapeHtml(master.publicId)}">Активировать</button>
        <button type="button" data-action="block-master" data-id="${escapeHtml(master.publicId)}">Заблокировать</button>
      </div>
    </article>
  `);
}

function legacyAccessRequestsTemplate() {
  const requests = recordsFromPayload(state.remote.masters);
  return cardListTemplate("Заявки на мастерский доступ", requests, (request) => `
    <article class="data-card">
      <div>
        <p class="eyebrow">${escapeHtml(request.requestedRole === "admin" ? "Шляпник" : "Мастер")} · ${formatDateTime(request.createdAt)}</p>
        <h3>${escapeHtml(request.displayName || "Новая заявка")}</h3>
        <p>${escapeHtml(request.telegramUsername || "")} · ${escapeHtml(request.email || "")}</p>
      </div>
      <div class="inline-actions">
        <button type="button" data-action="approve-master-access" data-id="${escapeHtml(request.publicId)}">Одобрить</button>
        <button type="button" data-action="reject-master-access" data-id="${escapeHtml(request.publicId)}">Отклонить</button>
      </div>
    </article>
  `);
}

function accessRequestsTemplate() {
  const access = state.remote.masterAccess || {};
  return `
    ${cardListTemplate("Ожидают подтверждения", access.pending || [], pendingAccessCard)}
    ${cardListTemplate("Активные мастера", access.active || [], activeAccessCard)}
    ${cardListTemplate("Заблокированные", access.blocked || [], blockedAccessCard)}
  `;
}

function pendingAccessCard(request) {
  return accessCard(request, `
    <button type="button" data-action="approve-master-access" data-id="${escapeHtml(request.publicId)}">Разрешить доступ</button>
    <button type="button" data-action="reject-master-access" data-id="${escapeHtml(request.publicId)}">Отклонить</button>
  `);
}

function activeAccessCard(request) {
  return accessCard(request, `
    <button type="button" data-action="revoke-master-access" data-id="${escapeHtml(request.publicId)}">Забрать доступ</button>
  `);
}

function blockedAccessCard(request) {
  return accessCard(request, `
    <button type="button" data-action="approve-master-access" data-id="${escapeHtml(request.publicId)}">Вернуть доступ</button>
  `);
}

function accessCard(request, actions) {
  return `
    <article class="data-card">
      <div>
        <p class="eyebrow">${escapeHtml(request.requestedRole === "admin" ? "Шляпник" : "Мастер")} · ${formatDateTime(request.createdAt)}</p>
        <h3>${escapeHtml(request.displayName || "Новая заявка")}</h3>
        <p>${escapeHtml(request.telegramUsername || "Telegram не указан")} · ${escapeHtml(request.email || "email не указан")}</p>
        <p>${escapeHtml(request.status || "")}</p>
      </div>
      <div class="inline-actions">${actions}</div>
    </article>
  `;
}

function profileTemplate() {
  return `
    <section class="profile-layout">
      <article class="overview-hero">
        <div class="overview-portrait">
          <img src="${state.account?.profileMode === "master" ? "/assets/images/masters/alexander.jpeg" : "./hatter-mark.png"}" alt="" />
        </div>
        <div>
          <p class="eyebrow">${state.role === "HATTER" ? "Владелец Таверны" : "Мастер Таверны"}</p>
          <h2>${escapeHtml(state.account?.displayName || "Мастер")}</h2>
          <p>Профиль связан с подтверждённым входом через дневник и Telegram. Публичное описание мастеров пока берётся из общего сайта, а кабинет уже готов к единому источнику профиля.</p>
        </div>
      </article>
      <div class="metric-grid">
        ${metricCard("Telegram", state.account?.telegramUsername || "указан при входе", "основной контакт")}
        ${metricCard("E-mail", state.account?.email || "указан при входе", "для входа")}
        ${metricCard("Режим", sessionLabel(), "активные права")}
      </div>
    </section>
  `;
}

function adminGalleryTemplate() {
  const posts = recordsFromPayload(state.remote.gallery);
  return cardListTemplate("Публикации галереи", posts, (post) => `
    <article class="data-card gallery-admin-card">
      <button type="button" class="gallery-preview" data-action="open-gallery-post" data-id="${escapeHtml(post.publicId)}" aria-label="Открыть публикацию ${escapeHtml(post.title || post.publicId)}">
        ${post.previewUrl
          ? `<img src="${escapeHtml(post.previewUrl)}" alt="${escapeHtml(post.title || "Публикация галереи")}" loading="lazy" />`
          : `<span>Нет фото</span>`}
      </button>
      <div>
        <p class="eyebrow">${escapeHtml(galleryTypeLabel(post.type))} · ${escapeHtml(galleryCategoryLabel(post.category))}</p>
        <h3>${escapeHtml(post.title || post.publicId)}</h3>
        <p>${formatDate(post.eventDate || post.publishedAt || post.createdAt)} · ${escapeHtml(post.mediaCount || 0)} фото · ${escapeHtml(post.authorName || "автор не указан")} · ${escapeHtml(post.status)}${post.visible ? "" : " / скрыто"}</p>
      </div>
      <div class="inline-actions">
        <button type="button" data-action="open-gallery-post" data-id="${escapeHtml(post.publicId)}">Открыть</button>
        <button type="button" data-action="publish-gallery-post" data-id="${escapeHtml(post.publicId)}">Опубликовать</button>
        <button type="button" data-action="hide-gallery-post" data-id="${escapeHtml(post.publicId)}">Скрыть</button>
        <button type="button" data-action="delete-gallery-post" data-id="${escapeHtml(post.publicId)}">Удалить</button>
      </div>
    </article>
  `);
}

function adminRatingTemplate() {
  const players = recordsFromPayload(state.remote.rating);
  const summary = players.reduce((acc, player) => {
    acc.games += Number(player.gamesPlayed || 0);
    acc.points += Number(player.totalPoints || 0);
    acc.inspiration += Number(player.inspirationCount || 0);
    return acc;
  }, { games: 0, points: 0, inspiration: 0 });
  return `
    <div class="metric-grid rating-summary">
      ${metricCard("Игроки", players.length, "в рейтинге")}
      ${metricCard("Игры", summary.games, "учтены")}
      ${metricCard("Очки", summary.points, "общая сумма")}
      ${metricCard("Вдохновение", summary.inspiration, "всего")}
    </div>
    ${cardListTemplate("Список игроков", players, adminRatingCard)}
  `;
}

function adminRatingCard(player) {
  const rankClass = Number(player.rank) <= 3 ? ` rank-${player.rank}` : "";
  const selected = state.selectedRatingId === player.publicId;
  return `
    <article class="rating-row${rankClass} ${selected ? "is-editing" : ""}">
      <button type="button" class="rating-row-main" data-action="edit-rating-player" data-id="${escapeHtml(player.publicId)}">
        <span class="rank">${escapeHtml(player.rank)}</span>
        <strong>${escapeHtml(player.displayName)}</strong>
        <em>${escapeHtml(player.nickname || "персонаж не указан")}</em>
        <span>${escapeHtml(player.gamesPlayed)} игр</span>
        <span>${escapeHtml(player.totalPoints)} очков</span>
        <span>${escapeHtml(player.inspirationCount)} вдохновения</span>
        <span>${escapeHtml(player.averagePointsPerGame ?? "0.00")} среднее</span>
      </button>
      ${selected ? ratingInlineEditor(player) : ""}
    </article>
  `;
}

function ratingInlineEditor(player) {
  return `
    <form class="rating-inline-editor" data-rating-editor-form>
      <input type="hidden" name="id" value="${escapeHtml(player.publicId)}" />
      <input type="hidden" name="currentGamesPlayed" value="${escapeHtml(player.gamesPlayed || 0)}" />
      <input type="hidden" name="currentTotalPoints" value="${escapeHtml(player.totalPoints || 0)}" />
      <input type="hidden" name="currentInspirationCount" value="${escapeHtml(player.inspirationCount || 0)}" />
      ${ratingInlineField("Игры", "gamesPlayed", player.gamesPlayed || 0)}
      ${ratingInlineField("Очки", "totalPoints", player.totalPoints || 0)}
      ${ratingInlineField("Вдохновение", "inspirationCount", player.inspirationCount || 0)}
      <label class="rating-reason">Причина
        <input name="reason" type="text" value="Правка из кабинета мастера" />
      </label>
      <div class="form-actions">
        <button type="button" data-action="save-rating-player">Сохранить</button>
        <button type="button" data-action="cancel-rating-edit">Отмена</button>
      </div>
    </form>
  `;
}

function ratingInlineField(label, name, value) {
  return `
    <label class="rating-number-field">${escapeHtml(label)}
      <span>
        <button type="button" data-action="rating-step" data-field="${escapeHtml(name)}" data-step="-1" aria-label="Уменьшить ${escapeHtml(label)}">−</button>
        <input name="${escapeHtml(name)}" type="number" min="0" step="1" value="${escapeHtml(value)}" />
        <button type="button" data-action="rating-step" data-field="${escapeHtml(name)}" data-step="1" aria-label="Увеличить ${escapeHtml(label)}">+</button>
      </span>
    </label>
  `;
}

function adminProfileTemplate() {
  const profile = state.remote.profile || {};
  if (state.profileEditing) return profileEditTemplate(profile);
  const photo = profile.photoUrl || (state.account?.profileMode === "master" ? "/assets/images/masters/alexander.jpeg" : "./hatter-mark.png");
  return `
    <section class="profile-page">
      <article class="profile-cover">
        <img src="${escapeHtml(photo)}" alt="Фото профиля ${escapeHtml(profile.displayName || state.account?.displayName || "мастера")}" />
        <div>
          <p class="eyebrow">${escapeHtml(profile.statusText || (state.role === "HATTER" ? "Владелец Таверны" : "Мастер Таверны"))}</p>
          <h2>${escapeHtml(profile.displayName || state.account?.displayName || "Мастер")}</h2>
          <p>${escapeHtml(profile.bio || "Профиль пока ждёт личную историю мастера.")}</p>
          <button type="button" data-action="edit-profile">Редактировать профиль</button>
        </div>
      </article>
      <div class="profile-info-grid">
        ${profileInfoCard("Как проводит игры", profile.style)}
        ${profileInfoCard("Вдохновение и интересы", profile.interests)}
        ${profileInfoCard("Системы", profile.systems)}
        ${profileInfoCard("Опыт", profile.experience)}
        ${profileInfoCard("Telegram", profile.telegramUsername)}
        ${profileInfoCard("Контакты", [profile.contactUrl, profile.phone, profile.extraLinks].filter(Boolean).join("\\n"))}
      </div>
    </section>
  `;
}

function modalTemplate() {
  if (state.galleryModal) return galleryModalTemplate(state.galleryModal);
  return "";
}

function galleryModalTemplate(post) {
  const media = Array.isArray(post.media) ? post.media : [];
  const hero = media[0];
  return `
    <div class="cabinet-modal" data-modal-backdrop role="dialog" aria-modal="true" aria-label="Публикация галереи">
      <article class="cabinet-modal__panel gallery-modal-panel">
        <button type="button" class="modal-close" data-action="close-modal" aria-label="Закрыть">×</button>
        ${hero ? `<img class="gallery-modal-image" src="${escapeHtml(hero.mediumUrl || hero.fileUrl || hero.thumbnailUrl)}" alt="${escapeHtml(hero.altText || post.title || "Фото галереи")}" />` : ""}
        <div class="gallery-modal-body">
          <p class="eyebrow">${escapeHtml(galleryTypeLabel(post.type))} · ${formatDate(post.eventDate || post.publishedAt || post.createdAt)}</p>
          <h2>${escapeHtml(post.title || "Публикация")}</h2>
          <p>${escapeHtml(post.description || "")}</p>
          ${post.storyHtml ? `<div class="post-story">${post.storyHtml}</div>` : ""}
          <p class="muted-line">Автор: ${escapeHtml(post.authorName || "не указан")} · статус: ${escapeHtml(post.status || "")}</p>
          ${media.length > 1 ? `<div class="gallery-modal-thumbs">${media.map((item) => `<img src="${escapeHtml(item.thumbnailUrl || item.mediumUrl || item.fileUrl)}" alt="${escapeHtml(item.altText || post.title || "Фото")}" loading="lazy" />`).join("")}</div>` : ""}
        </div>
      </article>
    </div>
  `;
}

function profileEditTemplate(profile) {
  return `
    <section class="work-panel profile-editor">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Профиль мастера</p>
          <h2>Редактировать страницу</h2>
        </div>
        <button type="button" data-action="cancel-profile-edit">Закрыть</button>
      </div>
      <form class="form-grid" data-profile-form>
        <label>Имя<input name="displayName" type="text" value="${escapeHtml(profile.displayName || "")}" /></label>
        <label>Telegram<input name="telegramUsername" type="text" value="${escapeHtml(profile.telegramUsername || "")}" /></label>
        <label>Ссылка для связи<input name="contactUrl" type="text" value="${escapeHtml(profile.contactUrl || "")}" /></label>
        <label>Телефон<input name="phone" type="text" value="${escapeHtml(profile.phone || "")}" /></label>
        <label class="form-wide">Короткий статус<input name="status" type="text" value="${escapeHtml(profile.statusText || "")}" /></label>
        <label class="form-wide">Фото профиля<input name="photo" type="file" accept="image/jpeg,image/png,image/webp" /></label>
        <label class="form-wide">О мастере<textarea name="bio" rows="5">${escapeHtml(profile.bio || "")}</textarea></label>
        <label class="form-wide">Как проводит игры<textarea name="style" rows="4">${escapeHtml(profile.style || "")}</textarea></label>
        <label class="form-wide">Чем вдохновляется<textarea name="interests" rows="4">${escapeHtml(profile.interests || "")}</textarea></label>
        <label>Игровые системы<input name="systems" type="text" value="${escapeHtml(profile.systems || "")}" /></label>
        <label>Опыт<input name="experience" type="text" value="${escapeHtml(profile.experience || "")}" /></label>
        <label class="form-wide">Дополнительные ссылки<textarea name="extraLinks" rows="3">${escapeHtml(profile.extraLinks || "")}</textarea></label>
        <div class="form-actions">
          <button type="button" data-action="save-profile">Сохранить профиль</button>
        </div>
      </form>
    </section>
  `;
}

function profileInfoCard(title, value) {
  return `
    <article class="metric profile-info-card">
      <span>${escapeHtml(title)}</span>
      <strong>${escapeHtml(value || "пока не заполнено")}</strong>
    </article>
  `;
}

function metricCard(label, value, hint) {
  return `<article class="metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong><small>${escapeHtml(hint || "")}</small></article>`;
}

function cardListTemplate(title, records, renderer) {
  return `
    <section class="card-list">
      <div class="panel-head">
        <h2>${escapeHtml(title)}</h2>
        <span>${records.length}</span>
      </div>
      ${records.length ? records.map(renderer).join("") : `<p class="empty-state">Записей пока нет.</p>`}
    </section>
  `;
}

function gameCard(game) {
  return `
    <article class="data-card">
      <div>
        <p class="eyebrow">${escapeHtml(game.gameSystem || "игра")} · ${formatDateTime(game.startsAt)}</p>
        <h3>${escapeHtml(game.title || "Без названия")}</h3>
        <p>${escapeHtml(game.masterName || "Мастер не указан")} · ${escapeHtml(game.status || "draft")}</p>
      </div>
      <div class="inline-actions">
        <button type="button" data-action="edit-game" data-id="${escapeHtml(game.id)}">Редактировать</button>
        <button type="button" data-action="publish-game" data-id="${escapeHtml(game.id)}">Опубликовать</button>
        <button type="button" data-action="cancel-game" data-id="${escapeHtml(game.id)}">Отменить</button>
        <button type="button" data-action="delete-game" data-id="${escapeHtml(game.id)}">В архив</button>
      </div>
    </article>
  `;
}

function ratingCard(player) {
  const rankClass = Number(player.rank) <= 3 ? ` rank-${player.rank}` : "";
  return `
    <article class="rating-row${rankClass}">
      <span class="rank">${escapeHtml(player.rank)}</span>
      <strong>${escapeHtml(player.displayName)}</strong>
      <em>${escapeHtml(player.nickname || "персонаж не указан")}</em>
      <span>${escapeHtml(player.gamesPlayed)} игр</span>
      <span>${escapeHtml(player.totalPoints)} очков</span>
      <span>${escapeHtml(player.inspirationCount)} вдохновения</span>
      <span>${escapeHtml(player.averagePointsPerGame ?? "0.00")} среднее</span>
    </article>
  `;
}

function bindEvents() {
  document.querySelectorAll("[data-section]").forEach((button) => {
    button.addEventListener("click", () => {
      state.section = button.dataset.section;
      window.location.hash = state.section;
      render();
      loadSectionData(state.section);
    });
  });
  document.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", () => runAction(button.dataset.action, button.closest("form"), button));
  });
  document.querySelector("[data-modal-backdrop]")?.addEventListener("click", (event) => {
    if (event.target === event.currentTarget) closeModal();
  });
}

async function runAction(action, form, sourceElement = null) {
  const body = form ? formJson(form) : {};
  if (action === "logout") {
    await logout();
    return;
  }
  if (action === "refresh") {
    await refreshVisibleData();
    return;
  }
  if (action === "mark-notifications-read") {
    markNotificationsRead();
    return;
  }
  if (action === "switch-mode") {
    await switchMode(sourceElement?.dataset.mode);
    return;
  }
  if (action === "close-modal") {
    closeModal();
    return;
  }
  if (action === "open-gallery-post") {
    await openGalleryPost(sourceElement?.dataset.id);
    return;
  }
  if (action === "edit-rating-player") {
    state.selectedRatingId = sourceElement?.dataset.id || "";
    render();
    return;
  }
  if (action === "cancel-rating-edit") {
    state.selectedRatingId = "";
    render();
    return;
  }
  if (action === "rating-step") {
    stepRatingInput(sourceElement);
    return;
  }
  if (action === "edit-profile") {
    state.profileEditing = true;
    render();
    return;
  }
  if (action === "cancel-profile-edit") {
    state.profileEditing = false;
    render();
    return;
  }
  if (action === "edit-game") {
    state.selectedGameId = sourceElement?.dataset.id || "";
    state.notice = state.selectedGameId ? "Игра открыта в редакторе." : "Не удалось открыть игру.";
    render();
    return;
  }
  if (action === "clear-game-editor") {
    state.selectedGameId = "";
    state.notice = "Редактор игры закрыт.";
    render();
    return;
  }
  if (needsConfirmation(action) && !window.confirm(confirmMessage(action))) {
    state.notice = "Действие отменено.";
    render();
    return;
  }
  state.notice = "Сохраняю изменения...";
  render();
  try {
    if (action === "create-game") {
      const game = await apiPost("/api/v1/admin/games", gamePayload(body), true);
      state.notice = `Игра создана: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "save-game") {
      const game = await apiPut(`/api/v1/admin/games/${body.id}`, gameEditPayload(body), true);
      state.selectedGameId = game.id;
      state.notice = `Игра сохранена: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "publish-game") {
      const game = await apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/publish`, {}, true);
      state.notice = `Игра опубликована: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "cancel-game") {
      const game = await apiPost(`/api/v1/admin/games/${sourceElement?.dataset.id}/cancel`, {}, true);
      state.notice = `Игра отменена: ${game.title || game.id}`;
      await loadSectionData("games");
    } else if (action === "delete-game") {
      await apiDelete(`/api/v1/admin/games/${sourceElement?.dataset.id}`);
      state.notice = "Игра перенесена в архив.";
      await loadSectionData("games");
    } else if (action === "publish-gallery-post") {
      const post = await apiPost(`/api/v1/admin/gallery/posts/${sourceElement?.dataset.id}/publish`, {}, true);
      state.notice = `Публикация открыта: ${post.title || post.publicId}`;
      await loadSectionData("gallery");
    } else if (action === "hide-gallery-post") {
      const post = await apiPost(`/api/v1/admin/gallery/posts/${sourceElement?.dataset.id}/hide`, {}, true);
      state.notice = `Публикация скрыта: ${post.title || post.publicId}`;
      await loadSectionData("gallery");
    } else if (action === "delete-gallery-post") {
      await apiDelete(`/api/v1/admin/gallery/posts/${sourceElement?.dataset.id}`);
      state.notice = "Публикация удалена.";
      await loadSectionData("gallery");
    } else if (action === "save-rating-player") {
      await apiPost(`/api/v1/admin/rating/players/${body.id}/adjust`, ratingPayload(body), true);
      state.selectedRatingId = "";
      state.notice = "Рейтинг игрока сохранён.";
      await loadSectionData("rating");
    } else if (action === "save-profile") {
      const profile = await saveProfile(form);
      state.remote.profile = profile;
      state.profileEditing = false;
      state.notice = "Профиль сохранён.";
    } else if (action === "contact-service-request") {
      const request = await apiPost(`/api/v1/admin/service-requests/${sourceElement?.dataset.id}/contact`, {}, true);
      state.notice = `Заявка взята в работу: ${request.publicId}`;
      await loadSectionData("applications");
    } else if (action === "close-service-request") {
      const request = await apiPost(`/api/v1/admin/service-requests/${sourceElement?.dataset.id}/close`, {}, true);
      state.notice = `Заявка закрыта: ${request.publicId}`;
      await loadSectionData("applications");
    } else if (action === "activate-master") {
      const master = await apiPost(`/api/v1/admin/masters/${sourceElement?.dataset.id}/activate`, {}, true);
      state.notice = `Мастер активирован: ${master.title || master.publicId}`;
      await loadSectionData("masters");
    } else if (action === "block-master") {
      const master = await apiPost(`/api/v1/admin/masters/${sourceElement?.dataset.id}/block`, {}, true);
      state.notice = `Мастер заблокирован: ${master.title || master.publicId}`;
      await loadSectionData("masters");
    } else if (action === "approve-master-access") {
      const request = await apiPost(`/api/v1/admin/master-access-requests/${sourceElement?.dataset.id}/approve`, {}, true);
      state.notice = `Доступ одобрен: ${request.displayName || request.publicId}`;
      await loadSectionData("masters");
    } else if (action === "reject-master-access") {
      const request = await apiPost(`/api/v1/admin/master-access-requests/${sourceElement?.dataset.id}/reject`, {}, true);
      state.notice = `Доступ отклонён: ${request.displayName || request.publicId}`;
      await loadSectionData("masters");
    } else if (action === "revoke-master-access") {
      const request = await apiPost(`/api/v1/admin/master-access-requests/${sourceElement?.dataset.id}/block`, {}, true);
      state.notice = `Доступ забран: ${request.displayName || request.publicId}`;
      await loadSectionData("masters");
    }
    await loadSectionData("overview");
  } catch (error) {
    state.notice = actionErrorMessage(action, error);
  } finally {
    render();
  }
}

async function logout() {
  try {
    await fetch(`${API_BASE}/api/auth/logout`, {
      method: "POST",
      credentials: "include",
      headers: { Accept: "application/json" }
    });
  } catch {
    // Локальный выход всё равно должен убрать мастерский доступ из браузера.
  }
  localStorage.removeItem(ACCESS_STORAGE_KEY);
  window.location.assign(new URL("/hatter-diary.html", window.location.origin).href);
}

async function switchMode(mode) {
  if (!mode) return;
  try {
    const session = await apiPut("/api/auth/session-mode", { mode });
    applySession(session);
    state.section = "overview";
    window.location.hash = "overview";
    state.notice = mode === "hatter" ? "Режим Шляпника включён." : "Режим мастера Александра включён.";
    render();
    await refreshVisibleData();
  } catch (error) {
    state.notice = error.status === 403 ? "Переключать режим может только Шляпник." : "Не удалось переключить режим.";
    render();
  }
}

async function openGalleryPost(id) {
  if (!id) return;
  try {
    state.galleryModal = await apiGet(`/api/v1/admin/gallery/posts/${id}`);
    render();
  } catch (error) {
    state.notice = error.status ? `Не удалось открыть публикацию: ${error.status}.` : "Сервер временно недоступен.";
    render();
  }
}

function closeModal() {
  state.galleryModal = null;
  state.selectedRatingId = "";
  render();
}

async function refreshVisibleData() {
  if (state.role === "PLAYER") return;
  await loadSectionData("overview");
  if (state.section !== "overview") await loadSectionData(state.section);
  if (state.role === "HATTER") await loadSectionData("masters");
}

function markNotificationsRead() {
  const latest = recentActions()[0];
  if (latest) {
    state.lastSeenAction = actionKey(latest);
    localStorage.setItem(NOTIFICATIONS_SEEN_KEY, state.lastSeenAction);
  }
  render();
}

async function restoreSession() {
  try {
    const session = await apiGet("/api/auth/session");
    if (!session?.accessGranted) {
      state.account = null;
      localStorage.removeItem(ACCESS_STORAGE_KEY);
      render();
      return;
    }
    applySession(session);
    if (!roles[state.role]?.includes(state.section)) state.section = initialSection(state.role);
    render();
    await refreshVisibleData();
  } catch {
    render();
  }
}

function applySession(session) {
  const role = session.systemRole || session.role || "player";
  state.account = {
    accessGranted: true,
    displayName: session.displayName || "Мастер Таверны",
    role: session.role || "master",
    baseRole: session.baseRole || session.role || "master",
    profileMode: session.profileMode || "master",
    canSwitchProfile: Boolean(session.canSwitchProfile),
    telegramUsername: session.telegramUsername || "",
    email: session.email || "",
    accountId: session.accountId || "",
    accountType: session.accountType || session.role || "",
    status: session.status || "active",
    systemRole: session.systemRole || "",
    activeProfile: session.activeProfile || session.profileMode || "",
    grantedAt: new Date().toISOString()
  };
  state.role = normalizeRole(role);
  if (!roles[state.role]?.includes(state.section)) state.section = initialSection(state.role);
  localStorage.setItem(ACCESS_STORAGE_KEY, JSON.stringify(state.account));
}

async function loadSectionData(section) {
  if (section === "masters") {
    state.loadingSection = section;
    try {
      const [pending, active, blocked] = await Promise.all([
        apiGet("/api/v1/admin/master-access-requests?status=pending"),
        apiGet("/api/v1/admin/master-access-requests?status=approved"),
        apiGet("/api/v1/admin/master-access-requests?status=blocked")
      ]);
      state.remote.masterAccess = {
        pending: recordsFromPayload(pending),
        active: recordsFromPayload(active),
        blocked: recordsFromPayload(blocked)
      };
    } catch (error) {
      state.notice = "Не удалось загрузить раздел. Попробуйте обновить страницу.";
    } finally {
      if (state.loadingSection === section) state.loadingSection = null;
      if (state.account?.accessGranted) render();
    }
    return;
  }
  const endpoint = sectionEndpoint(section);
  if (!endpoint) return;
  state.loadingSection = section;
  try {
    state.remote[remoteKey(section)] = await apiGet(endpoint);
  } catch (error) {
    state.notice = "Не удалось загрузить раздел. Попробуйте обновить страницу.";
  } finally {
    if (state.loadingSection === section) state.loadingSection = null;
    if (state.account?.accessGranted) render();
  }
}

function sectionEndpoint(section) {
  if (section === "overview") return "/api/v1/admin/dashboard";
  if (section === "games") return "/api/v1/admin/games?page=0&size=50";
  if (section === "applications") return "/api/v1/admin/data/applications?page=0&size=50";
  if (section === "gallery") return "/api/v1/admin/gallery/posts?page=0&size=50";
  if (section === "rating") return "/api/v1/admin/rating/players?page=0&size=50";
  if (section === "masters") return "/api/v1/admin/master-access-requests?status=pending";
  if (section === "profile") return "/api/v1/admin/profile";
  return null;
}

function remoteKey(section) {
  return section;
}

function filterOwnGames(games) {
  if (state.role === "HATTER") return games;
  const name = String(state.account?.displayName || "").toLowerCase();
  if (!name) return games;
  const own = games.filter((game) => String(game.masterName || "").toLowerCase().includes(name.replace("мастер ", "")));
  return own.length ? own : games;
}

async function apiGet(path) {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    headers: { Accept: "application/json" }
  });
  if (!response.ok) throw httpError("GET", path, response.status);
  return response.json();
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
  if (!response.ok) throw httpError("POST", path, response.status);
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

async function apiPut(path, body, csrf = false) {
  const headers = {
    Accept: "application/json",
    "Content-Type": "application/json"
  };
  if (csrf) headers["X-XSRF-TOKEN"] = await ensureCsrf();
  const response = await fetch(`${API_BASE}${path}`, {
    method: "PUT",
    credentials: "include",
    headers,
    body: JSON.stringify(body)
  });
  if (!response.ok) throw httpError("PUT", path, response.status);
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
  if (!response.ok) throw httpError("DELETE", path, response.status);
}

function httpError(method, path, status) {
  const error = new Error(`${method} ${path} failed with ${status}`);
  error.status = status;
  return error;
}

async function ensureCsrf() {
  try {
    await apiGet("/api/v1/auth/csrf");
  } catch {
    return "";
  }
  return decodeURIComponent(xsrfToken() || "");
}

function xsrfToken() {
  return document.cookie
    .split("; ")
    .find((part) => part.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
}

function recordsFromPayload(payload) {
  return Array.isArray(payload) ? payload : payload?.content || payload?.items || [];
}

function recentActions() {
  return state.remote.overview?.recentActions || recordsFromPayload(state.remote.notifications);
}

function humanAction(action) {
  const actor = humanActor(action.actor || action.user || action.userId);
  const entity = action.entityTitle || action.entityType || "запись";
  const map = {
    "game.created_from_cabinet": `${actor} создал игру`,
    "game.updated_from_cabinet": `${actor} изменил игру`,
    "game.published_from_cabinet": `${actor} опубликовал игру`,
    "game.cancelled_from_cabinet": `${actor} отменил игру`,
    "game.archived_from_cabinet": `${actor} перенёс игру в архив`,
    "gallery.post_published_from_cabinet": `${actor} открыл публикацию галереи`,
    "gallery.post_hidden_from_cabinet": `${actor} скрыл публикацию галереи`,
    "gallery.post_deleted_from_cabinet": `${actor} удалил публикацию галереи`,
    "rating.player_adjusted_from_cabinet": `${actor} изменил рейтинг игрока`,
    "site_account.master_approved_from_cabinet": `${actor} подтвердил мастерский доступ`,
    "site_account.master_rejected_from_cabinet": `${actor} отклонил мастерский доступ`,
    "site_account.master_blocked_from_cabinet": `${actor} отозвал мастерский доступ`,
    "master.access_approved_from_cabinet": `${actor} подтвердил заявку мастера`,
    "master.access_rejected_from_cabinet": `${actor} отклонил заявку мастера`,
    "master.access_blocked_from_cabinet": `${actor} забрал доступ мастера`,
    "service_request.contacted_from_cabinet": `${actor} взял заявку в работу`,
    "service_request.closed_from_cabinet": `${actor} закрыл заявку`,
    "master.profile_updated_from_cabinet": `${actor} обновил профиль мастера`,
    "master.profile_photo_updated_from_cabinet": `${actor} обновил фото мастера`
  };
  return map[action.action] || `${actor} изменил ${entity}`;
}

function humanActor(value) {
  const actor = String(value || "").trim();
  if (!actor) return "Таверна";
  if (actor === "master-cabinet") return "Кабинет таверны";
  if (actor === "telegram-bot") return "Писарь таверны";
  if (/^\d+$/.test(actor)) return "Писарь таверны";
  return actor;
}

function actionKey(action) {
  return `${action.createdAt || ""}|${action.action || ""}|${action.entityType || action.entity || ""}`;
}

function isUnread(action) {
  const key = actionKey(action);
  return key && (!state.lastSeenAction || key > state.lastSeenAction);
}

function unreadActions() {
  return recentActions().filter(isUnread);
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
    ageRating: body.ageRating,
    startsAt: new Date(body.startsAt).toISOString(),
    durationMinutes: Number(body.durationMinutes),
    minPlayers: Number(body.minPlayers),
    maxPlayers: Number(body.maxPlayers),
    price: Number(body.price),
    currency: body.currency,
    masterPublicId: body.masterPublicId,
    contactUrl: body.contactUrl,
    staffNotes: body.staffNotes
  };
}

function gameEditPayload(body) {
  return {
    title: optionalText(body.title),
    description: optionalText(body.description),
    gameSystem: optionalText(body.gameSystem),
    startsAt: body.startsAt ? new Date(body.startsAt).toISOString() : null,
    durationMinutes: optionalNumber(body.durationMinutes),
    minPlayers: optionalNumber(body.minPlayers),
    maxPlayers: optionalNumber(body.maxPlayers),
    price: optionalNumber(body.price),
    currency: optionalText(body.currency),
    masterPublicId: optionalText(body.masterPublicId),
    contactUrl: optionalText(body.contactUrl)
  };
}

function ratingPayload(body) {
  const currentGames = optionalNumber(body.currentGamesPlayed) || 0;
  const currentPoints = optionalNumber(body.currentTotalPoints) || 0;
  const currentInspiration = optionalNumber(body.currentInspirationCount) || 0;
  if (body.gamesPlayed !== undefined || body.totalPoints !== undefined || body.inspirationCount !== undefined) {
    return {
      gamesDelta: Math.max(0, optionalNumber(body.gamesPlayed) || 0) - currentGames,
      pointsDelta: Math.max(0, optionalNumber(body.totalPoints) || 0) - currentPoints,
      inspirationDelta: Math.max(0, optionalNumber(body.inspirationCount) || 0) - currentInspiration,
      reason: optionalText(body.reason) || "Правка из кабинета мастера"
    };
  }
  return {
    gamesDelta: optionalNumber(body.gamesDelta) || 0,
    pointsDelta: optionalNumber(body.pointsDelta) || 0,
    inspirationDelta: optionalNumber(body.inspirationDelta) || 0,
    reason: optionalText(body.reason) || "Правка из кабинета мастера"
  };
}

function stepRatingInput(button) {
  const form = button?.closest("[data-rating-editor-form]");
  const field = button?.dataset.field;
  if (!form || !field) return;
  const input = form.elements[field];
  if (!(input instanceof HTMLInputElement)) return;
  const current = Number(input.value || 0);
  input.value = String(Math.max(0, current + Number(button.dataset.step || 0)));
}

async function saveProfile(form) {
  const body = formJson(form);
  let photoUrl = state.remote.profile?.photoUrl || "";
  const file = form?.elements?.photo?.files?.[0];
  if (file) {
    const payload = new FormData();
    payload.append("file", file);
    const uploaded = await apiMultipart("/api/v1/admin/profile/photo", payload);
    photoUrl = uploaded.photoUrl || photoUrl;
  }
  return apiPut("/api/v1/admin/profile", {
    displayName: optionalText(body.displayName),
    telegramUsername: optionalText(body.telegramUsername),
    contactUrl: optionalText(body.contactUrl),
    photoUrl,
    status: optionalText(body.status),
    bio: optionalText(body.bio),
    style: optionalText(body.style),
    interests: optionalText(body.interests),
    systems: optionalText(body.systems),
    experience: optionalText(body.experience),
    phone: optionalText(body.phone),
    extraLinks: optionalText(body.extraLinks)
  }, true);
}

async function apiMultipart(path, body) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    credentials: "include",
    headers: {
      Accept: "application/json",
      "X-XSRF-TOKEN": await ensureCsrf()
    },
    body
  });
  if (!response.ok) throw httpError("POST", path, response.status);
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

function optionalNumber(value) {
  return value === null || value === undefined || String(value).trim() === "" ? null : Number(value);
}

function optionalText(value) {
  return value === null || value === undefined || String(value).trim() === "" ? null : String(value).trim();
}

function needsConfirmation(action) {
  return [
    "cancel-game",
    "delete-game",
    "hide-gallery-post",
    "delete-gallery-post",
    "close-service-request",
    "block-master",
    "revoke-master-access"
  ].includes(action);
}

function confirmMessage(action) {
  if (action === "revoke-master-access") return "Забрать доступ у этого мастера?";
  return {
    "cancel-game": "Отменить эту игру? Она исчезнет из активной афиши.",
    "delete-game": "Перенести игру в архив?",
    "hide-gallery-post": "Скрыть эту публикацию из галереи?",
    "delete-gallery-post": "Удалить эту публикацию окончательно?",
    "close-service-request": "Закрыть эту заявку?",
    "block-master": "Заблокировать этого мастера?"
  }[action] || "Подтвердить действие?";
}

function actionErrorMessage(action, error) {
  if (!error.status) return `${action}: сервер временно недоступен.`;
  if (error.status === 401 || error.status === 403) return "Недостаточно прав для этого действия.";
  return `${action}: сервер ответил ${error.status}.`;
}

function formatDateTime(value) {
  if (!value || Number.isNaN(Date.parse(value))) return "";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatDate(value) {
  if (!value || Number.isNaN(Date.parse(value))) return "";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "long",
    year: "numeric"
  }).format(new Date(value));
}

function defaultGameStart() {
  const start = new Date();
  start.setDate(start.getDate() + 7);
  start.setHours(19, 0, 0, 0);
  return start.toISOString().slice(0, 16);
}

function dateTimeLocalValue(value) {
  if (!value || Number.isNaN(Date.parse(value))) return "";
  const date = new Date(value);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}

function galleryTypeLabel(type) {
  return {
    photo: "Фото",
    story: "История",
    character_sheet: "Герой"
  }[type] || type || "";
}

function galleryCategoryLabel(category) {
  return {
    games: "Игры",
    events: "События",
    heroes: "Герои",
    tavern: "Таверна",
    miniatures: "Миниатюры",
    other: "Другое"
  }[category] || category || "";
}

function sessionLabel() {
  if (!state.account) return "доступ не открыт";
  if (state.role === "PLAYER") return "личный профиль";
  return state.role === "HATTER" ? "полный доступ" : "мастерский режим";
}

function loadTablePrefs() {
  try {
    return JSON.parse(localStorage.getItem(TABLE_PREFS_KEY) || "{}");
  } catch {
    return {};
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

render();
restoreSession();

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && (state.galleryModal || state.selectedRatingId)) {
    closeModal();
  }
});
