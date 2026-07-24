const rootPath = document.body.dataset.root || "";
const apiRoot = window.location.protocol === "file:" ? "http://localhost:4177/" : rootPath;
const masters = window.TAVERNA_MASTERS || [];
const hatterDiaryEntries = window.TAVERNA_HATTER_DIARY || [];
const siteSettings = {
  ADDRESS: "Могилёв, улица Ленинская, 29, третий этаж, направо от входа",
  ADDRESS_MAP_URL: "",
  PHONE_NUMBER: "",
  TELEGRAM_COMMUNITY_URL: "",
  TELEGRAM_COMMUNITY_LABEL: "Таверна Шляпника",
  TELEGRAM_COMMUNITY_DESCRIPTION: "Новости, анонсы, файлы.",
  WORKING_HOURS_WEEKDAYS: "Понедельник-пятница: 17:00-22:00",
  WORKING_HOURS_WEEKEND: "Суббота-воскресенье: 10:00-00:00",
  UNP: "",
  OPERATOR_NAME: "[ПОЛНОЕ НАИМЕНОВАНИЕ ОПЕРАТОРА]",
  OPERATOR_ADDRESS: "[ЮРИДИЧЕСКИЙ ИЛИ ПОЧТОВЫЙ АДРЕС]",
  PRIVACY_CONTACT: "[ЭЛЕКТРОННАЯ ПОЧТА ИЛИ ИНОЙ КОНТАКТ]",
  CONSENT_WITHDRAWAL_CONTACT: "[КОНТАКТ ДЛЯ ОТЗЫВА СОГЛАСИЯ]",
  ...(window.TAVERNA_SITE_SETTINGS || {})
};
const CONSENT_VERSION = "1.0";
const PRIVACY_POLICY_VERSION = "1.0";
const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
let games = [];
let ratingPlayers = [];
let activeDiaryIndex = 0;
let galleryPosts = [];
let activeGalleryMode = "photos";
let activeGalleryPostId = "";
let activeGalleryMediaIndex = 0;
let galleryModalTrigger = null;
let galleryTouchStartX = 0;
let galleryTouchStartY = 0;

const menuButton = document.querySelector("[data-menu-button]");
const nav = document.querySelector("[data-nav]");
const navBackdrop = document.querySelector("[data-nav-backdrop]");
const gallery = document.querySelector("[data-gallery]");
const galleryPrev = document.querySelector("[data-gallery-prev]");
const galleryNext = document.querySelector("[data-gallery-next]");
const modal = document.querySelector("[data-modal]");

function assetPath(path) {
  if (/^(https?:)?\/\//.test(path)) return path;
  return `${rootPath}${path}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function privacyPolicyUrl() {
  return assetPath("privacy-policy.html");
}

function ensureRatingNavLinks() {
  document.querySelectorAll(".main-nav").forEach((menu) => {
    const hasRating = Array.from(menu.querySelectorAll("a")).some((link) => link.getAttribute("href")?.includes("rating"));
    if (hasRating) return;
    const link = document.createElement("a");
    link.href = assetPath("rating.html");
    link.textContent = "Рейтинг";
    const galleryLink = Array.from(menu.querySelectorAll("a")).find((item) => item.getAttribute("href")?.includes("gallery"));
    menu.insertBefore(link, galleryLink || null);
  });
}

function sanitizePhone(value) {
  return String(value || "").replace(/[^\d+]/g, "");
}

function consentField(formType) {
  const id = `consent-${formType}-${Math.random().toString(36).slice(2)}`;
  return `
    <div class="consent-field form-wide">
      <label class="consent-label" for="${id}">
        <input id="${id}" type="checkbox" name="consentGiven" value="true" required aria-describedby="${id}-details">
        <span>Я даю согласие на обработку моих персональных данных для рассмотрения заявки, связи со мной и организации выбранной игры, услуги или мероприятия. Я ознакомился(ась) с <a href="${privacyPolicyUrl()}" target="_blank" rel="noreferrer">Политикой в отношении обработки персональных данных</a>.</span>
      </label>
      <p class="consent-details" id="${id}-details">
        <a href="${privacyPolicyUrl()}" target="_blank" rel="noreferrer">Подробнее об обработке данных</a>: оператор ${escapeHtml(siteSettings.OPERATOR_NAME)} обрабатывает указанные в форме контакты для связи и организации заявки. Срок хранения и контакт для отзыва согласия указаны в Политике.
      </p>
      <input type="hidden" name="consentVersion" value="${CONSENT_VERSION}">
      <input type="hidden" name="privacyPolicyVersion" value="${PRIVACY_POLICY_VERSION}">
    </div>
  `;
}

function setNav(open) {
  nav?.classList.toggle("open", open);
  navBackdrop?.classList.toggle("open", open);
  document.body.classList.toggle("nav-open", open);
  menuButton?.setAttribute("aria-expanded", String(open));
}

menuButton?.addEventListener("click", () => setNav(!nav?.classList.contains("open")));
navBackdrop?.addEventListener("click", () => setNav(false));
nav?.addEventListener("click", (event) => {
  if (event.target instanceof HTMLAnchorElement) setNav(false);
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeModal();
    setNav(false);
  }
});

function formatGameDate(date) {
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    weekday: "long",
    timeZone: "Europe/Minsk"
  }).format(date);
}

function formatGameDay(date) {
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "short",
    timeZone: "Europe/Minsk"
  }).format(date);
}

function formatGameTime(date) {
  return new Intl.DateTimeFormat("ru-RU", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Europe/Minsk"
  }).format(date);
}

function localDateKey(date) {
  return new Intl.DateTimeFormat("sv-SE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: "Europe/Minsk"
  }).format(date);
}

function startOfCurrentWeek() {
  const today = new Date();
  const monday = new Date(today);
  const dayOffset = (today.getDay() + 6) % 7;
  monday.setHours(0, 0, 0, 0);
  monday.setDate(today.getDate() - dayOffset);
  return monday;
}

function renderScheduleWeek() {
  const week = document.querySelector("[data-schedule-week]");
  if (!week) return;

  // Недельная строка афиши показывает только наличие игр, сами карточки остаются ниже.
  const monday = startOfCurrentWeek();
  const todayKey = localDateKey(new Date());
  const gamesByDay = games.reduce((acc, game) => {
    if (!(game.startsAt instanceof Date) || Number.isNaN(game.startsAt.getTime())) return acc;
    const key = localDateKey(game.startsAt);
    acc.set(key, (acc.get(key) || 0) + 1);
    return acc;
  }, new Map());
  const weekdayFormatter = new Intl.DateTimeFormat("ru-RU", { weekday: "short", timeZone: "Europe/Minsk" });
  const dayFormatter = new Intl.DateTimeFormat("ru-RU", { day: "numeric", timeZone: "Europe/Minsk" });
  const monthFormatter = new Intl.DateTimeFormat("ru-RU", { month: "short", timeZone: "Europe/Minsk" });

  week.innerHTML = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(monday);
    date.setDate(monday.getDate() + index);
    const key = localDateKey(date);
    const count = gamesByDay.get(key) || 0;
    const dots = Array.from({ length: Math.min(count, 4) }, () => `<i class="schedule-day-dot" aria-hidden="true"></i>`).join("");
    const label = count
      ? `${count} ${count === 1 ? "игра" : count < 5 ? "игры" : "игр"}`
      : "игр нет";
    return `
      <div class="schedule-day${key === todayKey ? " is-today" : ""}" aria-label="${escapeHtml(`${weekdayFormatter.format(date)}, ${dayFormatter.format(date)} ${monthFormatter.format(date)}: ${label}`)}">
        <span>${escapeHtml(weekdayFormatter.format(date))}</span>
        <strong>${escapeHtml(dayFormatter.format(date))}</strong>
        <span>${escapeHtml(monthFormatter.format(date))}</span>
        <div class="schedule-day-dots" aria-hidden="true">${dots}</div>
      </div>
    `;
  }).join("");
}

function normalizeApiGame(game) {
  const start = game.dateTimeStart ? new Date(game.dateTimeStart) : null;
  const duration = game.durationMinutes ? `${Math.round(game.durationMinutes / 60)} ч.` : "";
  const price = Number(game.price) > 0 ? `${game.price} ${game.currency || "BYN"} с человека` : "Бесплатно";
  const players = game.minPlayers && game.maxPlayers ? `${game.minPlayers}–${game.maxPlayers} игроков` : "";
  const availableSeats = Number(game.availableSeats ?? game.maxPlayers ?? 0);

  return {
    id: game.id,
    title: game.title,
    system: game.system || game.gameSystem || "",
    description: game.description || "",
    startsAt: start,
    dateLabel: start ? formatGameDate(start) : "",
    day: start ? formatGameDay(start) : "",
    time: start ? formatGameTime(start) : "",
    duration,
    players,
    price,
    level: game.experienceLevel || "",
    age: game.ageRating || "",
    masterName: game.master?.name || "",
    contactUrl: game.contactUrl || "",
    tags: game.tags || [],
    availableSeats,
    bookedSeats: Number(game.bookedSeats || 0),
    signupStatus: availableSeats > 0 ? `${availableSeats} свободных мест` : "Мест нет"
  };
}

function scheduleStatusElement(schedule) {
  let status = document.querySelector("[data-schedule-status]");
  if (!status) {
    status = document.createElement("div");
    status.className = "schedule-status";
    status.dataset.scheduleStatus = "";
    schedule.parentElement?.insertBefore(status, schedule);
  }
  return status;
}

function setScheduleStatus(schedule, text = "", retryText = "Обновить") {
  const status = scheduleStatusElement(schedule);
  if (!text) {
    status.hidden = true;
    status.innerHTML = "";
    return;
  }
  status.hidden = false;
  status.innerHTML = `
    <span>${escapeHtml(text)}</span>
    <button class="chip" type="button" data-refresh-schedule>${escapeHtml(retryText)}</button>
  `;
  status.querySelector("[data-refresh-schedule]")?.addEventListener("click", () => renderSchedule());
}

async function loadScheduleFromApi(schedule, options = {}) {
  if (!options.silent) setScheduleStatus(schedule);

  try {
    const response = await fetch(`${apiRoot}api/games?limit=50`, { headers: { Accept: "application/json" } });
    if (!response.ok) throw new Error(`API unavailable: ${response.status}`);
    const payload = await response.json();
    games = (payload.games || []).map(normalizeApiGame);
    setScheduleStatus(schedule);
    return true;
  } catch (error) {
    console.error("Schedule load failed", error);
    games = [];
    renderScheduleWeek();
    setScheduleStatus(schedule, "Не удалось загрузить афишу. Попробуйте обновить страницу немного позже.", "Повторить");
    schedule.innerHTML = `
      <article class="empty-state reveal">
        <h3>Не удалось загрузить афишу.</h3>
        <p>Попробуйте обновить страницу немного позже.</p>
      </article>
    `;
    initReveal();
    return false;
  }
}

function gameCard(game, index) {
  const descId = `game-desc-${escapeHtml(game.id)}`;
  const signupDisabled = game.availableSeats <= 0 ? " disabled" : "";
  const tags = (game.tags || []).join(" ");

  return `
    <article class="game-card reveal" data-type="${escapeHtml(tags)}" data-game-id="${escapeHtml(game.id)}">
      <div class="game-card-body">
        <div class="game-card-topline">
          <span>${escapeHtml(game.system)}</span>
          ${game.masterName ? `<span>${escapeHtml(game.masterName)}</span>` : ""}
        </div>
        <h3>${escapeHtml(game.title)}</h3>
        <div class="game-date-line">
          <strong>${escapeHtml(game.dateLabel)}</strong>
          ${game.time ? `<span>${escapeHtml(game.time)}</span>` : ""}
        </div>
        <p class="game-description is-collapsed" id="${descId}">${escapeHtml(game.description)}</p>
        ${game.description.length > 150 ? `<button class="text-button" type="button" data-toggle-desc aria-controls="${descId}" aria-expanded="false">Подробнее</button>` : ""}
        <div class="meta-row">
          ${game.duration ? `<span>${escapeHtml(game.duration)}</span>` : ""}
          ${game.players ? `<span>${escapeHtml(game.players)}</span>` : ""}
          ${game.price ? `<span>${escapeHtml(game.price)}</span>` : ""}
          ${game.level ? `<span>${escapeHtml(game.level)}</span>` : ""}
          ${game.age ? `<span>${escapeHtml(game.age)}</span>` : ""}
          <span>${escapeHtml(game.signupStatus)}</span>
        </div>
      </div>
      <div class="game-card-actions">
        <button class="button primary" type="button" data-open-signup="${escapeHtml(game.id)}"${signupDisabled}>Записаться на игру</button>
        ${game.contactUrl ? `<a class="button ghost" href="${escapeHtml(telegramSignupUrl(game))}" target="_blank" rel="noreferrer">Написать мастеру</a>` : ""}
      </div>
    </article>
  `;
}

function telegramSignupUrl(game) {
  const base = game.contactUrl || siteSettings.TELEGRAM_COMMUNITY_URL || "https://t.me/Taverna_Shlyapnika";
  const message = `Здравствуйте! Хочу записаться на игру "${game.title}" ${game.dateLabel} в ${game.time}.`;
  return `${base}${base.includes("?") ? "&" : "?"}text=${encodeURIComponent(message)}`;
}

async function renderSchedule(options = {}) {
  const schedule = document.querySelector("[data-schedule]");
  if (!schedule) return;

  const loaded = await loadScheduleFromApi(schedule, options);
  if (!loaded) return;
  renderScheduleWeek();

  if (!games.length) {
    schedule.innerHTML = `
      <article class="empty-state reveal">
        <h3>Пока нет запланированных игр.</h3>
        <p>Но новые истории уже готовятся. Загляните немного позже.</p>
      </article>
    `;
    initReveal();
    return;
  }

  schedule.innerHTML = games.map(gameCard).join("");
  initReveal();
}

document.querySelectorAll("[data-service-choice]").forEach((link) => {
  link.addEventListener("click", () => {
    setServiceChoice(link.dataset.serviceChoice || "", link.dataset.serviceType || "");
  });
});

function setServiceChoice(title, type) {
  const serviceInput = document.querySelector("[data-service-request-form] input[name='service']");
  const typeInput = document.querySelector("[data-service-request-form] input[name='serviceType']");
  const selectedService = document.querySelector("[data-selected-service]");

  if (serviceInput instanceof HTMLInputElement) serviceInput.value = title;
  if (typeInput instanceof HTMLInputElement) typeInput.value = type;
  document.querySelectorAll("[data-service-option]").forEach((option) => {
    const selected = option instanceof HTMLElement && option.dataset.serviceTitle === title;
    option.setAttribute("aria-selected", String(selected));
  });
  if (selectedService instanceof HTMLElement) {
    selectedService.textContent = title
      ? `Выбран формат: ${title}. Осталось оставить контакт и детали.`
      : "Выберите услугу в карточках выше или укажите её в форме.";
  }
}

document.querySelector("[data-service-picker]")?.addEventListener("click", (event) => {
  const option = event.target instanceof HTMLElement ? event.target.closest("[data-service-option]") : null;
  if (!(option instanceof HTMLElement)) return;
  setServiceChoice(option.dataset.serviceTitle || "", option.dataset.serviceType || "");
});

document.querySelector("[data-service-picker]")?.addEventListener("keydown", (event) => {
  if (!(event.target instanceof HTMLElement)) return;
  const option = event.target.closest("[data-service-option]");
  if (!(option instanceof HTMLElement)) return;
  if (event.key !== "Enter" && event.key !== " ") return;
  event.preventDefault();
  setServiceChoice(option.dataset.serviceTitle || "", option.dataset.serviceType || "");
});

document.querySelectorAll("[data-participant-stepper]").forEach((stepper) => {
  const input = stepper.querySelector("input[name='participants']");
  if (!(input instanceof HTMLInputElement)) return;

  const clamp = (value) => {
    const min = Number(input.min || 1);
    const max = Number(input.max || 200);
    const next = Number.isFinite(value) ? value : min;
    return Math.min(Math.max(next, min), max);
  };
  const updateButtons = () => {
    const value = clamp(Number(input.value || input.min || 1));
    stepper.querySelectorAll("[data-participant-step]").forEach((button) => {
      if (!(button instanceof HTMLButtonElement)) return;
      const step = Number(button.dataset.participantStep || 0);
      button.disabled = step < 0 ? value <= Number(input.min || 1) : value >= Number(input.max || 200);
    });
  };

  stepper.addEventListener("click", (event) => {
    const button = event.target instanceof HTMLElement ? event.target.closest("[data-participant-step]") : null;
    if (!(button instanceof HTMLButtonElement)) return;
    const step = Number(button.dataset.participantStep || 0);
    input.value = String(clamp(Number(input.value || input.min || 1) + step));
    updateButtons();
    input.dispatchEvent(new Event("change", { bubbles: true }));
  });

  input.addEventListener("change", () => {
    input.value = String(clamp(Number(input.value || input.min || 1)));
    updateButtons();
  });
  updateButtons();
});

function openModal(content, panelClass = "") {
  if (!modal) return;
  modal.innerHTML = `<div class="modal-panel ${escapeHtml(panelClass)}" role="dialog" aria-modal="true" tabindex="-1">${content}</div>`;
  modal.hidden = false;
  document.body.classList.add("modal-open");
  modal.querySelector("[data-modal-close]")?.addEventListener("click", closeModal);
  const panel = modal.querySelector(".modal-panel");
  if (panel instanceof HTMLElement) panel.focus({ preventScroll: true });
}

function closeModal() {
  if (!modal) return;
  modal.hidden = true;
  modal.innerHTML = "";
  document.body.classList.remove("modal-open");
  activeGalleryPostId = "";
  activeGalleryMediaIndex = 0;
  if (galleryModalTrigger instanceof HTMLElement && document.contains(galleryModalTrigger)) {
    galleryModalTrigger.focus({ preventScroll: true });
  }
  galleryModalTrigger = null;
}

modal?.addEventListener("click", (event) => {
  if (event.target === modal) closeModal();
});

document.addEventListener("click", (event) => {
  const target = event.target;
  if (!(target instanceof HTMLElement)) return;

  const toggle = target.closest("[data-toggle-desc]");
  if (toggle instanceof HTMLButtonElement) {
    const description = document.getElementById(toggle.getAttribute("aria-controls") || "");
    const expanded = toggle.getAttribute("aria-expanded") === "true";
    toggle.setAttribute("aria-expanded", String(!expanded));
    toggle.textContent = expanded ? "Подробнее" : "Свернуть";
    description?.classList.toggle("is-collapsed", expanded);
    return;
  }

  const galleryMode = target.closest("[data-gallery-mode]");
  if (galleryMode instanceof HTMLButtonElement) {
    activeGalleryMode = galleryMode.dataset.galleryMode === "stories" ? "stories" : "photos";
    renderGalleryPage();
    return;
  }

  const galleryCard = target.closest("[data-gallery-post]");
  if (galleryCard instanceof HTMLElement && !target.closest("[data-full-image]")) {
    const post = galleryPosts.find((item) => item.publicId === galleryCard.dataset.galleryPost);
    if (post) openGalleryPost(post, 0, galleryCard);
    return;
  }

  const imageButton = target.closest("[data-full-image]");
  if (imageButton instanceof HTMLElement) {
    openModal(`
      <button class="modal-close" type="button" data-modal-close aria-label="Закрыть">×</button>
      <img class="modal-image" src="${escapeHtml(imageButton.dataset.fullImage || "")}" alt="${escapeHtml(imageButton.dataset.fullAlt || "")}">
    `);
    return;
  }

  const prevPhotoButton = target.closest("[data-gallery-photo-prev]");
  if (prevPhotoButton instanceof HTMLButtonElement) {
    if (prevPhotoButton.disabled) return;
    shiftGalleryPhoto(-1);
    return;
  }

  const nextPhotoButton = target.closest("[data-gallery-photo-next]");
  if (nextPhotoButton instanceof HTMLButtonElement) {
    if (nextPhotoButton.disabled) return;
    shiftGalleryPhoto(1);
    return;
  }

  const galleryThumb = target.closest("[data-gallery-thumb]");
  if (galleryThumb instanceof HTMLElement) {
    activeGalleryMediaIndex = Number(galleryThumb.dataset.galleryThumb || 0);
    refreshGalleryModal();
    return;
  }

  if (target.closest("[data-gallery-post-prev]")) {
    shiftGalleryPost(-1);
    return;
  }

  if (target.closest("[data-gallery-post-next]")) {
    shiftGalleryPost(1);
    return;
  }

  const signupButton = target.closest("[data-open-signup]");
  if (signupButton instanceof HTMLButtonElement) {
    const game = games.find((item) => item.id === signupButton.dataset.openSignup);
    if (game) openSignupModal(game);
  }
});

document.addEventListener("keydown", (event) => {
  if (!activeGalleryPostId || modal?.hidden) return;
  if (event.key === "ArrowLeft") {
    event.preventDefault();
    shiftGalleryPhoto(-1);
  }
  if (event.key === "ArrowRight") {
    event.preventDefault();
    shiftGalleryPhoto(1);
  }
});

modal?.addEventListener("touchstart", (event) => {
  if (!activeGalleryPostId || !event.changedTouches.length) return;
  galleryTouchStartX = event.changedTouches[0].clientX;
  galleryTouchStartY = event.changedTouches[0].clientY;
}, { passive: true });

modal?.addEventListener("touchend", (event) => {
  if (!activeGalleryPostId || !event.changedTouches.length) return;
  const dx = event.changedTouches[0].clientX - galleryTouchStartX;
  const dy = event.changedTouches[0].clientY - galleryTouchStartY;
  if (Math.abs(dx) < 52 || Math.abs(dx) < Math.abs(dy) * 1.4) return;
  shiftGalleryPhoto(dx > 0 ? -1 : 1);
}, { passive: true });

document.querySelector("[data-gallery-page]")?.addEventListener("keydown", (event) => {
  if (event.key !== "Enter" && event.key !== " ") return;
  const target = event.target instanceof HTMLElement ? event.target.closest("[data-gallery-post]") : null;
  if (!(target instanceof HTMLElement)) return;
  event.preventDefault();
  const post = galleryPosts.find((item) => item.publicId === target.dataset.galleryPost);
  if (post) openGalleryPost(post, 0, target);
});

function openSignupModal(game) {
  openModal(`
    <button class="modal-close" type="button" data-modal-close aria-label="Закрыть">×</button>
    <p class="eyebrow">Запись на игру</p>
    <h2>${escapeHtml(game.title)}</h2>
    <p class="modal-note">${escapeHtml(game.dateLabel)} ${escapeHtml(game.time)} · ${escapeHtml(game.masterName)}</p>
    <form class="request-form" data-game-signup-form>
      <input type="hidden" name="gameId" value="${escapeHtml(game.id)}">
      <label>Имя<input name="playerName" required autocomplete="name"></label>
      <label>Telegram или телефон<input name="contact" required autocomplete="tel"></label>
      <label>Количество мест<input name="seats" type="number" min="1" max="${escapeHtml(game.availableSeats)}" value="1" required></label>
      <label class="form-wide">Комментарий<textarea name="comment" rows="4"></textarea></label>
      ${consentField("game-booking")}
      <button class="button primary" type="submit">Отправить заявку</button>
      <a class="button ghost" href="${escapeHtml(telegramSignupUrl(game))}" target="_blank" rel="noreferrer">Написать мастеру в Telegram</a>
      <p class="form-status" data-form-status></p>
    </form>
  `);
}

async function submitJson(url, form) {
  const status = form.querySelector("[data-form-status]");
  const button = form.querySelector("button[type='submit']");
  const payload = Object.fromEntries(new FormData(form).entries());
  const serviceInput = form.querySelector("input[name='service']");
  if (form.matches("[data-service-request-form]") && serviceInput instanceof HTMLInputElement && !serviceInput.value.trim()) {
    status.textContent = "Выберите услугу, которую хотите обсудить.";
    form.querySelector("[data-service-option]")?.focus();
    return;
  }
  const consent = form.querySelector("input[name='consentGiven']");
  if (consent instanceof HTMLInputElement && !consent.checked) {
    status.textContent = "Для отправки заявки необходимо дать согласие на обработку персональных данных.";
    consent.focus();
    return;
  }
  if (consent instanceof HTMLInputElement) payload.consentGiven = consent.checked;
  button.disabled = true;
  status.textContent = "Отправляем...";

  try {
    const response = await fetch(`${apiRoot}${url}`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.error || result.message || "Request failed");
    status.textContent = result.message || "Заявка сохранена.";
    form.reset();
    await renderSchedule();
  } catch (error) {
    console.error("Form submit failed", error);
    status.textContent = error instanceof Error ? error.message : "Не удалось отправить заявку.";
  } finally {
    button.disabled = false;
  }
}

document.addEventListener("submit", (event) => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;
  if (form.matches("[data-game-signup-form]")) {
    event.preventDefault();
    submitJson("api/game-signups", form);
  }
  if (form.matches("[data-service-request-form]")) {
    event.preventDefault();
    submitJson("api/service-requests", form);
  }
});

function masterPhoto(master) {
  const image = master.photo || master.image;
  if (image) return `<img loading="lazy" src="${assetPath(image)}" alt="Мастер ${escapeHtml(master.name)}">`;
  return `<span class="question-photo" aria-hidden="true">?</span>`;
}

function renderMasterBio(master) {
  const source = master.bio || master.fullDescription || master.description || master.shortDescription || "";
  const paragraphs = Array.isArray(source) ? source : String(source).split(/\n{2,}/).filter(Boolean);
  return paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("");
}

function renderMasterFacts(master) {
  if (!Array.isArray(master.facts) || master.facts.length === 0) return "";
  return `
    <ul class="master-facts" aria-label="Особенности мастера ${escapeHtml(master.name)}">
      ${master.facts.map((fact) => `<li>${escapeHtml(fact)}</li>`).join("")}
    </ul>
  `;
}

function renderMastersList() {
  const list = document.querySelector("[data-masters-list]");
  if (!list) return;

  list.innerHTML = masters.map((master) => `
    <a class="master-card reveal" href="${assetPath(master.page)}" aria-label="Подробнее о мастере ${escapeHtml(master.name)}">
      <div class="master-photo">${masterPhoto(master)}</div>
      <h3>${escapeHtml(master.name)}</h3>
      ${master.shortDescription || master.short ? `<p>${escapeHtml(master.shortDescription || master.short)}</p>` : ""}
      <span class="master-more">Подробнее о мастере</span>
    </a>
  `).join("");
  settleHashScroll("masters");
}

function settleHashScroll(id) {
  if (window.location.hash !== `#${id}`) return;
  const target = document.getElementById(id);
  if (!target) return;
  // Список мастеров появляется после JS-рендера, поэтому якорь аккуратно доводится повторно.
  window.setTimeout(() => {
    const headerHeight = document.querySelector("[data-header]")?.getBoundingClientRect().height || 0;
    const top = target.getBoundingClientRect().top + window.scrollY - headerHeight - 12;
    window.scrollTo({ top, behavior: prefersReducedMotion ? "auto" : "smooth" });
  }, 80);
}

function renderMasterPage() {
  const page = document.querySelector("[data-master-page]");
  if (!page) return;

  const id = document.body.dataset.masterId;
  const master = masters.find((item) => item.id === id) || masters[0];
  const image = master.photo || master.image;
  document.title = `${master.name} | Таверна Шляпника`;

  page.innerHTML = `
    <section class="master-hero reveal">
      <div class="master-hero-bg">${image ? `<img src="${assetPath(image)}" alt="Мастер ${escapeHtml(master.name)}">` : ""}</div>
      <div class="master-hero-overlay"></div>
      <div class="master-hero-content">
        <a class="back-link" href="${assetPath("index.html#masters")}">Вернуться к мастерам</a>
        <p class="eyebrow">${escapeHtml(master.title || "Мастер")}</p>
        <h1>${escapeHtml(master.name)}</h1>
        <p>${escapeHtml(master.shortDescription || master.description || "")}</p>
        <div class="hero-actions">
          <a class="button primary" href="${assetPath("index.html#games")}">К афише</a>
          <a class="button ghost" href="${assetPath("services.html")}">Услуги</a>
        </div>
      </div>
    </section>
    <section class="section master-details">
      <div class="detail-grid detail-grid-single">
        <article class="detail-card reveal">
          <h2>О мастере</h2>
          ${renderMasterBio(master)}
          ${renderMasterFacts(master)}
        </article>
      </div>
    </section>
  `;
}

function renderContactBlock() {
  const contact = document.querySelector("[data-contact-block]");
  if (!contact) return;

  const phone = sanitizePhone(siteSettings.PHONE_NUMBER);
  const communityDescription = siteSettings.TELEGRAM_COMMUNITY_DESCRIPTION || "Новости, анонсы, файлы.";
  const phoneCard = phone
    ? `<a class="contact-link" href="tel:${escapeHtml(phone)}"><span class="contact-icon" aria-hidden="true">ТЛ</span><span class="contact-link-body"><span>Телефон</span><strong>${escapeHtml(siteSettings.PHONE_NUMBER)}</strong></span></a>`
    : `<div class="contact-link contact-link-muted" role="note"><span class="contact-icon" aria-hidden="true">ТЛ</span><span class="contact-link-body"><span>Телефон</span><strong>Шляпник пока изучает устройство под названием “мобильный телефон”. Номер появится здесь, как только он разберётся, с какой стороны его держать.</strong></span></div>`;

  const communityCard = siteSettings.TELEGRAM_COMMUNITY_URL
    ? `<a class="contact-link" href="${escapeHtml(siteSettings.TELEGRAM_COMMUNITY_URL)}" target="_blank" rel="noreferrer"><span class="contact-icon" aria-hidden="true">TG</span><span class="contact-link-body"><span>Telegram-группа</span><strong>${escapeHtml(siteSettings.TELEGRAM_COMMUNITY_LABEL)}</strong><small>${escapeHtml(communityDescription)}</small></span></a>`
    : `<div class="contact-link contact-link-muted" role="note"><span class="contact-icon" aria-hidden="true">TG</span><span class="contact-link-body"><span>Telegram-группа</span><strong>${escapeHtml(siteSettings.TELEGRAM_COMMUNITY_LABEL)}</strong><small>${escapeHtml(communityDescription)}</small><em>[ССЫЛКА НА TELEGRAM-ГРУППУ]</em></span></div>`;

  const addressCard = siteSettings.ADDRESS_MAP_URL
    ? `<a class="contact-link" href="${escapeHtml(siteSettings.ADDRESS_MAP_URL)}" target="_blank" rel="noreferrer"><span class="contact-icon" aria-hidden="true">АД</span><span class="contact-link-body"><span>Адрес</span><strong>${escapeHtml(siteSettings.ADDRESS)}</strong><small>Открыть геолокацию на карте</small></span></a>`
    : `<div class="contact-link" role="note"><span class="contact-icon" aria-hidden="true">АД</span><span class="contact-link-body"><span>Адрес</span><strong>${escapeHtml(siteSettings.ADDRESS)}</strong></span></div>`;

  contact.innerHTML = `
    <div class="contacts-panel reveal">
      <div class="contacts-intro">
        <p class="eyebrow">Контакты</p>
        <h2 id="contacts-title">Контакты</h2>
        <p>Дверь Таверны открывается по расписанию историй. Здесь собраны официальные каналы связи, карта и новости.</p>
      </div>
      <div class="contact-grid">
        ${addressCard}
        <a class="contact-link" href="https://www.instagram.com/taverna_shlyapnika/" target="_blank" rel="noreferrer">
          <span class="contact-icon" aria-hidden="true">IG</span>
          <span class="contact-link-body"><span>Instagram</span><strong>@taverna_shlyapnika</strong></span>
        </a>
        ${communityCard}
        ${phoneCard}
        <div class="contact-link" role="note">
          <span class="contact-icon" aria-hidden="true">ВР</span>
          <span class="contact-link-body">
            <span>Режим работы</span>
            <strong>${escapeHtml(siteSettings.WORKING_HOURS_WEEKDAYS)}</strong>
            <small>${escapeHtml(siteSettings.WORKING_HOURS_WEEKEND)}</small>
          </span>
        </div>
        <a class="contact-link" href="${privacyPolicyUrl()}">
          <span class="contact-icon" aria-hidden="true">PD</span>
          <span class="contact-link-body"><span>Персональные данные</span><strong>Политика обработки</strong></span>
        </a>
      </div>
    </div>
  `;
}

function formatGalleryDate(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "Europe/Minsk"
  }).format(date).replace(/\s*г\.$/, "");
}

function galleryStatusNode() {
  return document.querySelector("[data-gallery-status]");
}

function setGalleryStatus(text) {
  const status = galleryStatusNode();
  if (status) status.textContent = text || "";
}

function galleryPostIndex(publicId) {
  return galleryVisiblePosts().findIndex((item) => item.publicId === publicId);
}

function isGalleryStory(post) {
  return post?.type === "story";
}

function galleryVisiblePosts() {
  return galleryPosts.filter((post) => activeGalleryMode === "stories" ? isGalleryStory(post) : !isGalleryStory(post));
}

function updateGalleryModeTabs() {
  document.querySelectorAll("[data-gallery-mode]").forEach((tab) => {
    const isActive = tab.getAttribute("data-gallery-mode") === activeGalleryMode;
    tab.classList.toggle("is-active", isActive);
    tab.setAttribute("aria-selected", isActive ? "true" : "false");
  });
}

function galleryMedia(post) {
  return Array.isArray(post?.media) ? post.media.filter((item) => item && (item.thumbnailUrl || item.mediumUrl || item.fileUrl)) : [];
}

function galleryImageUrl(item, preferred = "medium") {
  if (!item) return "";
  if (preferred === "thumb") return item.thumbnailUrl || item.mediumUrl || item.fileUrl || "";
  if (preferred === "original") return item.fileUrl || item.mediumUrl || item.thumbnailUrl || "";
  return item.mediumUrl || item.fileUrl || item.thumbnailUrl || "";
}

function galleryCardSpan(media) {
  const width = Number(media?.width || 0);
  const height = Number(media?.height || 0);
  if (!width || !height) return 34;
  const previewWidth = 230;
  const rowSize = 20;
  return Math.max(16, Math.ceil(((previewWidth * height) / width + 16) / rowSize));
}

function resizeGalleryMasonry() {
  const grid = document.querySelector("[data-gallery-content]");
  if (!(grid instanceof HTMLElement) || !grid.classList.contains("gallery-masonry")) return;
  const styles = window.getComputedStyle(grid);
  const rowHeight = parseFloat(styles.gridAutoRows) || 6;
  const rowGap = parseFloat(styles.rowGap) || 16;
  grid.querySelectorAll("[data-gallery-post]").forEach((card) => {
    if (!(card instanceof HTMLElement)) return;
    card.style.gridRowEnd = "";
    window.requestAnimationFrame(() => {
      const height = card.getBoundingClientRect().height;
      const span = Math.max(12, Math.ceil((height + rowGap) / (rowHeight + rowGap)));
      card.style.gridRowEnd = `span ${span}`;
    });
  });
}

function bindGalleryMasonryImages() {
  const grid = document.querySelector("[data-gallery-content]");
  if (!(grid instanceof HTMLElement)) return;
  grid.querySelectorAll("img").forEach((image) => {
    if (!(image instanceof HTMLImageElement) || image.complete) return;
    image.addEventListener("load", resizeGalleryMasonry, { once: true });
    image.addEventListener("error", resizeGalleryMasonry, { once: true });
  });
  window.requestAnimationFrame(resizeGalleryMasonry);
}

function galleryMetaRows(post) {
  const rows = [];
  const published = formatGalleryDate(post.publishedAt || post.createdAt);
  const eventDate = formatGalleryDate(post.eventDate);
  if (published) rows.push(`Добавлено ${published}`);
  if (eventDate) rows.push(`Событие состоялось ${eventDate}`);
  if (post.master?.name) rows.push(`Мастер: ${post.master.name}`);
  return rows;
}

function renderGalleryCard(post) {
  const media = galleryMedia(post);
  const cover = media[0];
  const src = galleryImageUrl(cover, "medium");
  const alt = cover?.altText || post.title || "Фотография из галереи Таверны";
  const width = cover?.width ? ` width="${escapeHtml(cover.width)}"` : "";
  const height = cover?.height ? ` height="${escapeHtml(cover.height)}"` : "";
  const storyClass = isGalleryStory(post) ? " gallery-post-card-story" : "";
  const rowSpan = galleryCardSpan(cover);

  return `
    <article class="gallery-post-card${storyClass}${src ? "" : " gallery-post-card-text"}" style="--gallery-row-span: ${rowSpan}; --gallery-row-span-mobile: ${Math.max(14, Math.round(rowSpan * 0.82))};" tabindex="0" role="button" data-gallery-post="${escapeHtml(post.publicId)}" aria-label="Открыть публикацию ${escapeHtml(post.title || "галереи")}">
      ${src ? `
        <img src="${escapeHtml(src)}" alt="${escapeHtml(alt)}" loading="lazy" decoding="async"${width}${height}>
      ` : `
        <div class="archive-placeholder" aria-hidden="true"><span></span></div>
      `}
    </article>
  `;
}

function galleryPostModal(post, mediaIndex = 0) {
  const media = galleryMedia(post);
  const activeIndex = media.length ? Math.min(Math.max(mediaIndex, 0), media.length - 1) : 0;
  const active = media[activeIndex];
  const activeSrc = galleryImageUrl(active, "medium");
  const activeAlt = active?.altText || post.title || "Фотография из галереи Таверны";
  const isStory = isGalleryStory(post);
  const storyBg = isStory ? (activeSrc || galleryImageUrl(media[0], "medium")) : "";
  const visiblePosts = galleryVisiblePosts();
  const postIndex = galleryPostIndex(post.publicId);
  const prevPost = postIndex > 0 ? visiblePosts[postIndex - 1] : null;
  const nextPost = postIndex >= 0 && postIndex < visiblePosts.length - 1 ? visiblePosts[postIndex + 1] : null;
  const canGoPrevPhoto = media.length > 1 || Boolean(prevPost);
  const canGoNextPhoto = media.length > 1 || Boolean(nextPost);
  const meta = galleryMetaRows(post);

  return `
    <button class="modal-close gallery-modal-close" type="button" data-modal-close aria-label="Закрыть публикацию">×</button>
    <article class="gallery-modal-post${isStory ? " gallery-modal-post-story" : ""}" data-gallery-modal-post="${escapeHtml(post.publicId)}" data-gallery-media-index="${activeIndex}"${storyBg ? ` style="--gallery-story-bg: url('${escapeHtml(storyBg)}')"` : ""}>
      ${activeSrc ? `
        <figure class="gallery-modal-figure">
          <img src="${escapeHtml(activeSrc)}" alt="${escapeHtml(activeAlt)}" loading="eager" decoding="async">
          <button class="gallery-photo-nav gallery-photo-nav-prev" type="button" data-gallery-photo-prev aria-label="Предыдущее фото"${canGoPrevPhoto ? "" : " disabled aria-disabled=\"true\""}>‹</button>
          <button class="gallery-photo-nav gallery-photo-nav-next" type="button" data-gallery-photo-next aria-label="Следующее фото"${canGoNextPhoto ? "" : " disabled aria-disabled=\"true\""}>›</button>
          ${media.length > 1 ? `<figcaption>${activeIndex + 1} / ${media.length}</figcaption>` : ""}
        </figure>
      ` : ""}

      <div class="gallery-modal-body">
        ${post.title ? `<h2>${escapeHtml(post.title)}</h2>` : ""}
        ${meta.length ? `<div class="gallery-modal-meta">${meta.map((item) => `<span>${escapeHtml(item)}</span>`).join("")}</div>` : ""}
        ${post.description ? `<p class="gallery-modal-description">${escapeHtml(post.description)}</p>` : ""}
        ${post.storyHtml ? `<div class="gallery-story">${post.storyHtml}</div>` : ""}
        ${media.length > 1 ? `
          <div class="gallery-thumbs" aria-label="Кадры публикации">
            ${media.map((item, index) => `
              <button class="gallery-thumb" type="button" data-gallery-thumb="${index}" aria-current="${index === activeIndex ? "true" : "false"}" aria-label="Показать фото ${index + 1}">
                <img src="${escapeHtml(galleryImageUrl(item, "thumb"))}" alt="" loading="lazy" decoding="async">
              </button>
            `).join("")}
          </div>
        ` : ""}
        <nav class="gallery-post-nav" aria-label="Навигация по публикациям">
          ${prevPost ? `<button type="button" data-gallery-post-prev>← ${escapeHtml(prevPost.title || "Предыдущая публикация")}</button>` : `<span></span>`}
          ${nextPost ? `<button type="button" data-gallery-post-next>${escapeHtml(nextPost.title || "Следующая публикация")} →</button>` : `<span></span>`}
        </nav>
      </div>
    </article>
  `;
}

function refreshGalleryModal() {
  if (!modal || !activeGalleryPostId) return;
  const post = galleryPosts.find((item) => item.publicId === activeGalleryPostId);
  const panel = modal.querySelector(".modal-panel");
  if (!post || !(panel instanceof HTMLElement)) return;
  panel.innerHTML = galleryPostModal(post, activeGalleryMediaIndex);
  panel.querySelector("[data-modal-close]")?.addEventListener("click", closeModal);
  panel.scrollTop = 0;
}

function openGalleryPost(post, mediaIndex = 0, trigger = null) {
  activeGalleryPostId = post.publicId;
  activeGalleryMediaIndex = mediaIndex;
  galleryModalTrigger = trigger instanceof HTMLElement ? trigger : document.activeElement;
  openModal(galleryPostModal(post, mediaIndex), "gallery-modal-panel");
  preloadNextGalleryImage(post, mediaIndex);
}

function shiftGalleryPhoto(direction) {
  const post = galleryPosts.find((item) => item.publicId === activeGalleryPostId);
  const media = galleryMedia(post);
  if (!post) return;
  if (media.length < 2) {
    shiftGalleryPost(direction);
    return;
  }
  activeGalleryMediaIndex = (activeGalleryMediaIndex + direction + media.length) % media.length;
  refreshGalleryModal();
  preloadNextGalleryImage(post, activeGalleryMediaIndex);
}

function shiftGalleryPost(direction) {
  const index = galleryPostIndex(activeGalleryPostId);
  const next = galleryVisiblePosts()[index + direction];
  if (!next) return;
  activeGalleryPostId = next.publicId;
  activeGalleryMediaIndex = 0;
  refreshGalleryModal();
  preloadNextGalleryImage(next, 0);
}

function preloadNextGalleryImage(post, mediaIndex) {
  const media = galleryMedia(post);
  if (media.length < 2) return;
  const next = media[(mediaIndex + 1) % media.length];
  const src = galleryImageUrl(next, "medium");
  if (!src) return;
  const image = new Image();
  image.src = src;
}

async function loadGalleryPosts() {
  setGalleryStatus("Загружаем галерею...");
  const response = await fetch(`${apiRoot}api/gallery`, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error(`Gallery API unavailable: ${response.status}`);
  const payload = await response.json();
  galleryPosts = Array.isArray(payload.posts) ? payload.posts : [];
  setGalleryStatus("");
}

async function renderGalleryPage() {
  const page = document.querySelector("[data-gallery-page]");
  const content = document.querySelector("[data-gallery-content]");
  if (!page || !content) return;

  try {
    await loadGalleryPosts();
    updateGalleryModeTabs();
    const visiblePosts = galleryVisiblePosts();
    const emptyText = activeGalleryMode === "stories"
      ? "Истории Таверны ещё ждут своих первых чернил."
      : "Истории Таверны ещё ждут своих первых следов.";
    content.innerHTML = visiblePosts.length
      ? visiblePosts.map(renderGalleryCard).join("")
      : `
        <article class="empty-state">
          <p>${emptyText}</p>
        </article>
      `;
    bindGalleryMasonryImages();
  } catch (error) {
    console.error("Gallery load failed", error);
    galleryPosts = [];
    setGalleryStatus("");
    content.innerHTML = `
      <article class="empty-state">
        <h3>Не удалось загрузить галерею.</h3>
        <p>Попробуйте обновить страницу немного позже.</p>
      </article>
    `;
  }

  initReveal();
}

function renderDiaryPage() {
  const entryNode = document.querySelector("[data-diary-entry]");
  const archiveNode = document.querySelector("[data-diary-archive]");
  const currentNode = document.querySelector("[data-diary-current]");
  if (!entryNode) return;

  const entries = Array.isArray(hatterDiaryEntries) ? hatterDiaryEntries : [];
  const entry = entries[activeDiaryIndex];
  if (!entry) {
    entryNode.innerHTML = `
      <p class="eyebrow">Пустая страница</p>
      <h2>Дневник пока закрыт</h2>
      <p>Когда появятся настоящие записи Шляпника, они будут храниться в отдельном файле данных.</p>
    `;
    if (archiveNode) archiveNode.innerHTML = "";
    if (currentNode) currentNode.textContent = "Страница 0 из 0";
    return;
  }

  const paragraphs = Array.isArray(entry.paragraphs) && entry.paragraphs.length
    ? entry.paragraphs
    : [entry.content || ""].filter(Boolean);
  const isIntro = entry.type === "intro";
  const inkVariants = ["ink-top-right", "ink-bottom-left", "ink-signature", "ink-side-drops", "ink-page-corner", "ink-small-drops-right"];
  const inkClass = entry.inkVariant ? `ink-${entry.inkVariant}` : inkVariants[activeDiaryIndex % inkVariants.length];

  entryNode.innerHTML = `
    <div class="diary-ink ${inkClass}" aria-hidden="true"></div>
    <p class="eyebrow">${escapeHtml(entry.date || entry.mood || "Запись")}</p>
    <span class="diary-page-number">${isIntro ? "Начало записей" : `Страница ${escapeHtml(entry.pageNumber || activeDiaryIndex)}`}</span>
    <h2>${escapeHtml(entry.title)}</h2>
    ${entry.subtitle ? `<p class="diary-excerpt">${escapeHtml(entry.subtitle)}</p>` : ""}
    <div class="diary-text${isIntro ? " diary-intro-text" : ""}">
      ${paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}
    </div>
    ${entry.signature ? `<p class="diary-signature">${escapeHtml(entry.signature)}</p>` : ""}
    ${entry.closing ? `<p class="diary-closing">${escapeHtml(entry.closing)}</p>` : ""}
  `;

  if (archiveNode) {
    archiveNode.innerHTML = `
      <div class="diary-archive-head">
        <p class="eyebrow">Закрытая полка</p>
        <span>${entries.length} записей из исходного дневника</span>
      </div>
      <div class="diary-list">
        ${entries.map((item, index) => `
          <button type="button" class="diary-list-item" data-diary-index="${index}" aria-current="${index === activeDiaryIndex ? "true" : "false"}">
            <span>${escapeHtml(item.pageNumber || index + 1)}</span>
            <strong>${escapeHtml(item.title)}</strong>
          </button>
        `).join("")}
      </div>
    `;
  }

  const prev = document.querySelector("[data-diary-prev]");
  const next = document.querySelector("[data-diary-next]");
  prev?.toggleAttribute("disabled", activeDiaryIndex === 0 || entries.length < 2);
  prev?.classList.toggle("is-hidden", activeDiaryIndex === 0);
  next?.toggleAttribute("disabled", entries.length < 2);
  if (currentNode) currentNode.textContent = isIntro ? "Начало записей" : `Запись ${activeDiaryIndex} из ${entries.length - 1}`;
}

document.querySelector("[data-diary-page]")?.addEventListener("click", (event) => {
  const target = event.target instanceof HTMLElement ? event.target : null;
  if (!target) return;
  const entries = Array.isArray(hatterDiaryEntries) ? hatterDiaryEntries : [];
  if (!entries.length) return;

  if (target.closest("[data-diary-prev]")) {
    activeDiaryIndex = (activeDiaryIndex - 1 + entries.length) % entries.length;
    renderDiaryPage();
  }
  if (target.closest("[data-diary-next]")) {
    activeDiaryIndex = (activeDiaryIndex + 1) % entries.length;
    renderDiaryPage();
  }
  const archiveButton = target.closest("[data-diary-index]");
  if (archiveButton instanceof HTMLElement) {
    activeDiaryIndex = Number(archiveButton.dataset.diaryIndex || 0);
    renderDiaryPage();
  }
});

document.querySelector("[data-open-diary]")?.addEventListener("click", () => {
  window.location.href = assetPath("hatter-diary.html");
});

function ratingStatusElement() {
  return document.querySelector("[data-rating-status]");
}

function setRatingStatus(text, retryText = "Обновить") {
  const status = ratingStatusElement();
  if (!status) return;
  if (!text) {
    status.hidden = true;
    status.innerHTML = "";
    return;
  }
  status.hidden = false;
  status.innerHTML = `
    <span>${escapeHtml(text)}</span>
    <button class="chip" type="button" data-refresh-rating>${escapeHtml(retryText)}</button>
  `;
  status.querySelector("[data-refresh-rating]")?.addEventListener("click", () => renderRatingPage());
}

function ratingAverage(value) {
  return Number(value || 0).toLocaleString("ru-RU", { minimumFractionDigits: 1, maximumFractionDigits: 2 });
}

function ratingDate(value) {
  if (!value) return "Пока нет";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "Europe/Minsk"
  }).format(new Date(value));
}

async function loadRatingFromApi(options = {}) {
  if (!options.silent) setRatingStatus("");
  try {
    const response = await fetch(`${apiRoot}api/rating?limit=200`, { headers: { Accept: "application/json" } });
    if (!response.ok) throw new Error(`Rating API unavailable: ${response.status}`);
    const payload = await response.json();
    ratingPlayers = Array.isArray(payload.players) ? payload.players : [];
    setRatingStatus("");
    return true;
  } catch (error) {
    console.error("Rating load failed", error);
    ratingPlayers = [];
    setRatingStatus("Не удалось загрузить рейтинг. Попробуйте обновить страницу немного позже.", "Повторить");
    return false;
  }
}

function filteredRatingPlayers() {
  const searchInput = document.querySelector("[data-rating-search]");
  const search = searchInput instanceof HTMLInputElement ? searchInput.value.trim().toLowerCase() : "";
  const filtered = ratingPlayers.filter((player) => {
    if (!search) return true;
    return `${player.displayName || ""} ${player.nickname || ""}`.toLowerCase().includes(search);
  });
  return filtered.sort((a, b) => a.rank - b.rank);
}

function ratingRow(player) {
  const rank = Number(player.rank || 0);
  return `
    <article class="rating-row reveal ${rank === 1 ? "rating-row-gold" : rank === 2 ? "rating-row-silver" : rank === 3 ? "rating-row-bronze" : ""}">
      <div class="rating-rank">${rank > 0 ? escapeHtml(rank) : "—"}</div>
      <div class="rating-player-name"><strong>${escapeHtml(player.displayName || "Игрок")}</strong></div>
      <div class="rating-character-name">${player.nickname ? escapeHtml(player.nickname) : "—"}</div>
      <div class="rating-stat" data-label="Игры"><strong>${escapeHtml(player.gamesPlayed)}</strong></div>
      <div class="rating-stat" data-label="Очки"><strong>${escapeHtml(player.totalPoints)}</strong></div>
      <div class="rating-stat" data-label="Вдохновение"><strong>${escapeHtml(player.inspirationCount)}</strong></div>
      <div class="rating-stat" data-label="Среднее"><strong>${ratingAverage(player.averagePointsPerGame)}</strong></div>
    </article>
  `;
}

function ratingListHeader() {
  return `
    <div class="rating-row rating-row-head" aria-hidden="true">
      <div>Место</div>
      <div>Игрок</div>
      <div>Персонаж</div>
      <div>Игры</div>
      <div>Очки</div>
      <div>Вдохновение</div>
      <div>Среднее</div>
    </div>
  `;
}

async function renderRatingPage(options = {}) {
  const page = document.querySelector("[data-rating-page]");
  if (!page) return;

  const loaded = await loadRatingFromApi(options);
  const list = page.querySelector("[data-rating-list]");
  if (!list) return;

  if (!loaded) {
    list.innerHTML = `
      <article class="empty-state reveal">
        <h3>Не удалось загрузить рейтинг.</h3>
        <p>Попробуйте обновить страницу немного позже.</p>
      </article>
    `;
    initReveal();
    return;
  }

  if (!ratingPlayers.length) {
    list.innerHTML = `
      <article class="empty-state reveal">
        <h3>Первые имена ещё не появились в летописи.</h3>
        <p>Когда мастера добавят игроков через Telegram-бота, рейтинг появится здесь автоматически.</p>
      </article>
    `;
    initReveal();
    return;
  }

  const players = filteredRatingPlayers();
  list.innerHTML = players.length
    ? ratingListHeader() + players.map(ratingRow).join("")
    : `
      <article class="empty-state reveal">
        <h3>По этому запросу никого не нашлось.</h3>
        <p>Попробуйте другое имя или псевдоним.</p>
      </article>
    `;
  initReveal();
}

function renderFooter() {
  const footer = document.querySelector("[data-site-footer]");
  if (!footer) return;
  const unp = siteSettings.UNP ? `УНП: ${escapeHtml(siteSettings.UNP)}` : "УНП: будет добавлен после оформления документов";

  footer.innerHTML = `
    <div class="footer-grid">
      <div class="footer-about">
        <a class="footer-brand" href="${assetPath("index.html")}">Таверна Шляпника</a>
        <p>D&D и настольно-ролевые игры в Могилёве.</p>
      </div>
      <nav aria-label="Навигация в футере">
        <a href="${assetPath("index.html#top")}">Главная</a>
        <a href="${assetPath("index.html#games")}">Афиша</a>
        <a href="${assetPath("services.html")}">Услуги</a>
        <a href="${assetPath("index.html#masters")}">Мастера</a>
        <a href="${assetPath("rating.html")}">Рейтинг</a>
        <a href="${assetPath("gallery.html")}">Галерея</a>
        <a href="${assetPath("dnd-simple.html")}">D&D простым языком</a>
      </nav>
      <div class="footer-contacts">
        <strong>Контакты</strong>
        <a href="${assetPath("index.html#contacts")}">Раздел контактов</a>
        <span>${escapeHtml(siteSettings.ADDRESS)}</span>
        ${siteSettings.ADDRESS_MAP_URL ? `<a href="${escapeHtml(siteSettings.ADDRESS_MAP_URL)}" target="_blank" rel="noreferrer">Открыть на карте</a>` : ""}
        ${siteSettings.TELEGRAM_COMMUNITY_URL ? `<a href="${escapeHtml(siteSettings.TELEGRAM_COMMUNITY_URL)}" target="_blank" rel="noreferrer">${escapeHtml(siteSettings.TELEGRAM_COMMUNITY_LABEL)}</a><span>${escapeHtml(siteSettings.TELEGRAM_COMMUNITY_DESCRIPTION || "Новости, анонсы, файлы.")}</span>` : ""}
        <span>${escapeHtml(siteSettings.WORKING_HOURS_WEEKDAYS)}</span>
        <span>${escapeHtml(siteSettings.WORKING_HOURS_WEEKEND)}</span>
      </div>
      <div class="footer-legal">
        <strong>Документы</strong>
        <a href="${privacyPolicyUrl()}">Политика обработки персональных данных</a>
      </div>
    </div>
    <p class="copyright">2026 · Таверна Шляпника · ${unp}</p>
  `;
}

function scrollGallery(direction) {
  if (!gallery) return;
  const amount = Math.max(260, Math.round(gallery.clientWidth * 0.72));
  gallery.scrollBy({ left: direction * amount, behavior: prefersReducedMotion ? "auto" : "smooth" });
}

galleryPrev?.addEventListener("click", () => scrollGallery(-1));
galleryNext?.addEventListener("click", () => scrollGallery(1));

document.querySelector("[data-rating-search]")?.addEventListener("input", () => {
  const list = document.querySelector("[data-rating-list]");
  if (!list) return;
  const players = filteredRatingPlayers();
  list.innerHTML = players.length
    ? ratingListHeader() + players.map(ratingRow).join("")
    : `
      <article class="empty-state reveal">
        <h3>По этому запросу никого не нашлось.</h3>
        <p>Попробуйте другое имя или псевдоним.</p>
      </article>
    `;
  initReveal();
});

document.querySelectorAll("[data-refresh-rating]").forEach((button) => {
  button.addEventListener("click", () => renderRatingPage());
});

document.addEventListener("click", async (event) => {
  const button = event.target instanceof HTMLElement ? event.target.closest("[data-copy]") : null;
  if (!(button instanceof HTMLButtonElement)) return;
  const value = button.dataset.copy || "";
  try {
    await navigator.clipboard.writeText(value);
    button.textContent = "Контакт скопирован";
  } catch {
    button.textContent = value;
  }
  window.setTimeout(() => {
    button.textContent = "Скопировать контакт";
  }, 1800);
});

function initReveal() {
  const elements = document.querySelectorAll(".reveal");
  if (prefersReducedMotion || !("IntersectionObserver" in window)) {
    elements.forEach((element) => element.classList.add("visible"));
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("visible");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12 });

  elements.forEach((element, index) => {
    element.style.setProperty("--reveal-delay", `${Math.min(index * 45, 240)}ms`);
    observer.observe(element);
  });
}

ensureRatingNavLinks();
renderSchedule();
renderRatingPage();
renderMastersList();
renderMasterPage();
renderContactBlock();
renderGalleryPage();
renderDiaryPage();
renderFooter();
initReveal();

if (document.querySelector("[data-schedule]")) {
  window.setInterval(() => renderSchedule({ silent: true }), 45_000);
}

if (document.querySelector("[data-rating-page]")) {
  window.setInterval(() => renderRatingPage({ silent: true }), 60_000);
}

if (document.querySelector("[data-gallery-page]")) {
  window.addEventListener("resize", () => window.requestAnimationFrame(resizeGalleryMasonry));
  window.setInterval(() => renderGalleryPage(), 60_000);
}
