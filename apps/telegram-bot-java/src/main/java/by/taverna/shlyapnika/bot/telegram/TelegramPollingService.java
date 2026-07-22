package by.taverna.shlyapnika.bot.telegram;

import by.taverna.shlyapnika.bot.backend.BackendApiClient;
import by.taverna.shlyapnika.bot.backend.BackendGameRequest;
import by.taverna.shlyapnika.bot.backend.BackendGameUpdateRequest;
import by.taverna.shlyapnika.bot.backend.BackendMasterRequest;
import by.taverna.shlyapnika.bot.backend.BackendMasterResponse;
import by.taverna.shlyapnika.bot.config.BotProperties;
import by.taverna.shlyapnika.bot.health.BotStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class TelegramPollingService {
  private static final Logger log = LoggerFactory.getLogger(TelegramPollingService.class);

  private final BotProperties properties;
  private final TelegramApiClient telegram;
  private final BackendApiClient backend;
  private final BotSessionStore sessions;
  private final BotStatus status;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private long offset = 0;

  public TelegramPollingService(
      BotProperties properties,
      TelegramApiClient telegram,
      BackendApiClient backend,
      BotSessionStore sessions,
      BotStatus status
  ) {
    this.properties = properties;
    this.telegram = telegram;
    this.backend = backend;
    this.sessions = sessions;
    this.status = status;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (!properties.tokenConfigured()) {
      log.warn("Telegram bot token is not configured; Java bot stays disabled");
      return;
    }
    if (!properties.polling()) {
      log.info("Telegram bot mode={} is configured; polling loop is not started", properties.mode());
      return;
    }
    if (!running.compareAndSet(false, true)) return;
    telegram.deleteWebhook();
    status.markRunning(true);
    executor.submit(this::pollLoop);
    log.info("Java Telegram bot polling started");
  }

  @PreDestroy
  public void stop() {
    running.set(false);
    status.markRunning(false);
    executor.shutdownNow();
    log.info("Java Telegram bot polling stopped");
  }

  private void pollLoop() {
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        var updates = telegram.getUpdates(offset);
        if (updates.isArray()) {
          for (var update : updates) {
            handleUpdate(update);
          }
        }
      } catch (Exception error) {
        log.warn("Telegram polling iteration failed", error);
        sleepAfterError();
      }
    }
  }

  private void handleUpdate(JsonNode update) {
    var updateId = update.path("update_id").asLong();
    if (updateId >= offset) offset = updateId + 1;
    status.markUpdate(updateId);
    log.info("Telegram update received updateId={}", updateId);

    try {
      var message = update.path("message");
      if (!message.isMissingNode()) {
        handleMessage(message);
        return;
      }

      var callback = update.path("callback_query");
      if (!callback.isMissingNode()) {
        telegram.answerCallback(callback.path("id").asText(""));
        handleCallback(callback);
      }
    } catch (IllegalArgumentException | IllegalStateException error) {
      var chatId = update.path("message").path("chat").path("id").asLong(update.path("callback_query").path("message").path("chat").path("id").asLong());
      if (chatId != 0) telegram.sendMessage(chatId, error.getMessage(), mainMenu());
      log.warn("Telegram update rejected updateId={} message={}", updateId, error.getMessage());
    } catch (Exception error) {
      log.warn("Telegram update handling failed updateId={}", updateId, error);
    }
  }

  private void handleMessage(JsonNode message) {
    var chatId = message.path("chat").path("id").asLong();
    var from = message.path("from");
    var userId = from.path("id").asLong();
    var text = BotTextParser.clean(message.path("text").asText(""));
    if (userId == 0 || text.isBlank()) return;

    switch (text) {
      case "/cancel" -> {
        sessions.reset(userId);
        telegram.sendMessage(chatId, "Отменено.", mainMenu());
      }
      case "/start" -> sendStart(chatId, userId, username(from));
      case "/register" -> beginRegistration(chatId, userId);
      case "/create_game" -> beginGameDraft(chatId, userId);
      case "/my_games" -> showMasterGames(chatId, userId);
      default -> handleStatefulText(chatId, userId, username(from), text);
    }
  }

  private void handleCallback(JsonNode callback) {
    var data = callback.path("data").asText("");
    var message = callback.path("message");
    var chatId = message.path("chat").path("id").asLong();
    var from = callback.path("from");
    var userId = from.path("id").asLong();
    if (userId == 0 || data.isBlank()) return;

    switch (data) {
      case "menu" -> sendStart(chatId, userId, username(from));
      case "register" -> beginRegistration(chatId, userId);
      case "create_game" -> beginGameDraft(chatId, userId);
      case "my_games" -> showMasterGames(chatId, userId);
      case "cancel" -> {
        sessions.reset(userId);
        telegram.sendMessage(chatId, "Отменено.", mainMenu());
      }
      case "use_profile_contact" -> useProfileContact(chatId, userId);
      case "manual_game_contact" -> {
        var session = sessions.get(userId);
        sessions.save(userId, "create:contact_manual", session.draft());
        telegram.sendMessage(chatId, "Введите контакт для записи в формате @username или https://t.me/username.", cancelKeyboard());
      }
      case "confirm_game" -> createGameFromDraft(chatId, userId);
      default -> handleDynamicCallback(chatId, userId, data);
    }
  }

  private void handleDynamicCallback(long chatId, long userId, String data) {
    if (data.startsWith("cancel_game:")) {
      var gameId = data.substring("cancel_game:".length());
      telegram.sendMessage(chatId, "Вы уверены, что хотите отменить игру? Она исчезнет из активной афиши.", confirmCancelKeyboard(gameId));
      return;
    }
    if (data.startsWith("confirm_cancel:")) {
      cancelGame(chatId, userId, data.substring("confirm_cancel:".length()));
      return;
    }
    if (data.startsWith("edit_game:")) {
      showEditFields(chatId, userId, data.substring("edit_game:".length()));
      return;
    }
    if (data.startsWith("edit_field:")) {
      beginEditField(chatId, userId, data);
      return;
    }
    handleOptionCallback(chatId, userId, data);
  }

  private void handleOptionCallback(long chatId, long userId, String data) {
    var session = sessions.get(userId);
    var draft = session.draft();
    if (data.startsWith("duration:")) {
      var minutes = Integer.parseInt(data.substring("duration:".length()));
      draft.durationMinutes(minutes == 0 ? null : minutes);
      sessions.save(userId, "create:description", draft);
      telegram.sendMessage(chatId, "Кратко расскажите, о чём будет эта игра.", cancelKeyboard());
      return;
    }
    if ("duration_manual".equals(data)) {
      sessions.save(userId, "create:duration_manual", draft);
      telegram.sendMessage(chatId, "Введите продолжительность в часах, например 3.5.", cancelKeyboard());
      return;
    }
    if (data.startsWith("date:")) {
      draft.date(data.substring("date:".length()));
      sessions.save(userId, "create:time", draft);
      telegram.sendMessage(chatId, "Укажите время начала игры в формате HH:MM, например 18:30.", cancelKeyboard());
      return;
    }
    if ("date_manual".equals(data)) {
      sessions.save(userId, "create:date_manual", draft);
      telegram.sendMessage(chatId, "Введите дату в формате YYYY-MM-DD.", cancelKeyboard());
      return;
    }
    if (data.startsWith("system:")) {
      var value = data.substring("system:".length());
      if ("manual".equals(value)) {
        sessions.save(userId, "create:system_manual", draft);
        telegram.sendMessage(chatId, "Введите игровую систему.", cancelKeyboard());
        return;
      }
      draft.gameSystem(value);
      sessions.save(userId, "create:experience", draft);
      telegram.sendMessage(chatId, "Для кого подходит игра?", optionKeyboard("experience", List.of("без опыта", "для новичков", "любой уровень", "для опытных игроков")));
      return;
    }
    if (data.startsWith("experience:")) {
      var value = data.substring("experience:".length());
      if ("manual".equals(value)) {
        sessions.save(userId, "create:experience_manual", draft);
        telegram.sendMessage(chatId, "Введите уровень игроков.", cancelKeyboard());
        return;
      }
      draft.experienceLevel(value);
      sessions.save(userId, "create:age", draft);
      telegram.sendMessage(chatId, "Выберите возрастное ограничение.", optionKeyboard("age", List.of("12+", "14+", "16+", "18+", "без ограничения")));
      return;
    }
    if (data.startsWith("age:")) {
      var value = data.substring("age:".length());
      if ("manual".equals(value)) {
        sessions.save(userId, "create:age_manual", draft);
        telegram.sendMessage(chatId, "Введите возрастное ограничение.", cancelKeyboard());
        return;
      }
      draft.ageRating(value);
      sessions.save(userId, "create:contact", draft);
      telegram.sendMessage(chatId, "Использовать контакт мастера для записи или указать другой?", contactChoiceKeyboard());
    }
  }

  private void sendStart(long chatId, long userId, String telegramUsername) {
    var master = backend.findMasterByTelegram(userId);
    if (master == null) {
      telegram.sendMessage(
          chatId,
          "Добро пожаловать в систему мастеров Таверны Шляпника.\n\nЧтобы создавать игры для афиши, зарегистрируйтесь как мастер.",
          startKeyboard()
      );
      return;
    }
    telegram.sendMessage(chatId, "Добро пожаловать обратно, " + master.displayName() + ".", mainMenu());
  }

  private void beginRegistration(long chatId, long userId) {
    sessions.save(userId, "register:name", new GameDraft());
    telegram.sendMessage(chatId, "Как вас представить игрокам? Напишите имя или мастерский псевдоним.", cancelKeyboard());
  }

  private void beginGameDraft(long chatId, long userId) {
    var master = requireActiveMaster(chatId, userId);
    if (master == null) return;
    var draft = new GameDraft();
    draft.contactUrl(master.contactUrl());
    sessions.save(userId, "create:title", draft);
    telegram.sendMessage(chatId, "Напишите название игры.", cancelKeyboard());
  }

  private BackendMasterResponse requireActiveMaster(long chatId, long userId) {
    var master = backend.findMasterByTelegram(userId);
    if (master == null) {
      telegram.sendMessage(chatId, "Сначала нужно зарегистрироваться как мастер.", startKeyboard());
      return null;
    }
    if (!master.active()) {
      telegram.sendMessage(chatId, "Ваш профиль заблокирован администратором. Публикация игр недоступна.");
      return null;
    }
    return master;
  }

  private void handleStatefulText(long chatId, long userId, String telegramUsername, String text) {
    var session = sessions.get(userId);
    var draft = session.draft();
    try {
      if (session.state().startsWith("edit:")) {
        updateGameField(chatId, userId, session.state(), text);
        return;
      }

      switch (session.state()) {
        case "register:name" -> {
          draft.displayName(BotTextParser.shortText(text, "Имя мастера", 2, 80));
          sessions.save(userId, "register:contact", draft);
          telegram.sendMessage(chatId, "Укажите Telegram-контакт для связи: @username или https://t.me/username.", cancelKeyboard());
        }
        case "register:contact" -> {
          draft.contactUrl(BotTextParser.contact(text));
          var master = backend.upsertMaster(new BackendMasterRequest(userId, telegramUsername, draft.displayName(), draft.contactUrl()));
          sessions.reset(userId);
          telegram.sendMessage(chatId, "Готово. Регистрация завершена, теперь можно создавать игры.", mainMenu());
          log.info("Master registered via Java bot masterId={} telegramUserId={}", master.id(), userId);
        }
        case "create:title" -> {
          draft.title(BotTextParser.title(text));
          sessions.save(userId, "create:date", draft);
          telegram.sendMessage(chatId, "Выберите дату проведения игры.", dateKeyboard());
        }
        case "create:date_manual" -> {
          draft.date(text);
          sessions.save(userId, "create:time", draft);
          telegram.sendMessage(chatId, "Укажите время начала игры в формате HH:MM, например 18:30.", cancelKeyboard());
        }
        case "create:time" -> {
          BotTextParser.validateDateTime(draft.date(), text);
          draft.time(text);
          sessions.save(userId, "create:duration", draft);
          telegram.sendMessage(chatId, "Укажите предполагаемую продолжительность игры.", durationKeyboard());
        }
        case "create:duration_manual" -> {
          draft.durationMinutes(BotTextParser.durationMinutes(text));
          sessions.save(userId, "create:description", draft);
          telegram.sendMessage(chatId, "Кратко расскажите, о чём будет эта игра.", cancelKeyboard());
        }
        case "create:description" -> {
          draft.description(BotTextParser.description(text));
          sessions.save(userId, "create:players", draft);
          telegram.sendMessage(chatId, "Сколько игроков вы планируете принять? Можно указать диапазон, например 3-5.", cancelKeyboard());
        }
        case "create:players" -> {
          var players = BotTextParser.players(text);
          draft.minPlayers(players.minPlayers());
          draft.maxPlayers(players.maxPlayers());
          sessions.save(userId, "create:price", draft);
          telegram.sendMessage(chatId, "Укажите стоимость участия с одного человека, например 35 BYN. Для бесплатной игры: 0 BYN.", cancelKeyboard());
        }
        case "create:price" -> {
          var price = BotTextParser.price(text);
          draft.price(price.amount());
          draft.currency(price.currency());
          sessions.save(userId, "create:system", draft);
          telegram.sendMessage(chatId, "По какой игровой системе будет проходить игра?", optionKeyboard("system", List.of("Dungeons & Dragons", "Call of Cthulhu", "собственная система")));
        }
        case "create:system_manual" -> {
          draft.gameSystem(BotTextParser.shortText(text, "Игровая система", 2, 80));
          sessions.save(userId, "create:experience", draft);
          telegram.sendMessage(chatId, "Для кого подходит игра?", optionKeyboard("experience", List.of("без опыта", "для новичков", "любой уровень", "для опытных игроков")));
        }
        case "create:experience_manual" -> {
          draft.experienceLevel(BotTextParser.shortText(text, "Уровень игроков", 2, 80));
          sessions.save(userId, "create:age", draft);
          telegram.sendMessage(chatId, "Выберите возрастное ограничение.", optionKeyboard("age", List.of("12+", "14+", "16+", "18+", "без ограничения")));
        }
        case "create:age_manual" -> {
          draft.ageRating(BotTextParser.shortText(text, "Возрастное ограничение", 2, 30));
          sessions.save(userId, "create:contact", draft);
          telegram.sendMessage(chatId, "Использовать контакт мастера для записи или указать другой?", contactChoiceKeyboard());
        }
        case "create:contact_manual" -> {
          draft.contactOverride(BotTextParser.contact(text));
          var master = requireActiveMaster(chatId, userId);
          if (master == null) return;
          sessions.save(userId, "create:preview", draft);
          telegram.sendMessage(chatId, gamePreview(draft, master.contactUrl()), publishKeyboard());
        }
        default -> telegram.sendMessage(chatId, "Я вас слышу. Используйте меню или команды /register, /create_game и /my_games.", mainMenu());
      }
    } catch (IllegalArgumentException error) {
      telegram.sendMessage(chatId, error.getMessage(), cancelKeyboard());
    }
  }

  private void useProfileContact(long chatId, long userId) {
    var master = requireActiveMaster(chatId, userId);
    if (master == null) return;
    var session = sessions.get(userId);
    var draft = session.draft();
    sessions.save(userId, "create:preview", draft);
    telegram.sendMessage(chatId, gamePreview(draft, master.contactUrl()), publishKeyboard());
  }

  private void createGameFromDraft(long chatId, long userId) {
    var master = requireActiveMaster(chatId, userId);
    if (master == null) return;
    var draft = sessions.get(userId).draft();
    var contactUrl = draft.contactOverride() == null ? master.contactUrl() : draft.contactOverride();
    var result = backend.createGame(new BackendGameRequest(
        master.id(),
        draft.title(),
        draft.description(),
        draft.gameSystem(),
        draft.experienceLevel(),
        draft.ageRating(),
        draft.date(),
        draft.time(),
        draft.durationMinutes(),
        draft.minPlayers(),
        draft.maxPlayers(),
        draft.price(),
        draft.currency(),
        contactUrl
    ));
    sessions.reset(userId);
    var game = result.game();
    var message = "published".equals(game.status())
        ? "Игра опубликована и добавлена в афишу."
        : "Заявка создана и ждёт подтверждения администратора.";
    telegram.sendMessage(chatId, message + "\n\n" + game.title() + "\n" + game.startsAtLabel(), mainMenu());
  }

  private void showMasterGames(long chatId, long userId) {
    var master = requireActiveMaster(chatId, userId);
    if (master == null) return;
    var games = backend.listMasterGames(master.id(), "upcoming").games();
    if (games == null || games.isEmpty()) {
      telegram.sendMessage(chatId, "Пока нет предстоящих игр.", mainMenu());
      return;
    }
    for (var game : games) {
      var seats = game.maxPlayers() == null ? "" : "\nСвободно мест: " + game.availableSeats() + " из " + game.maxPlayers();
      var text = game.title() + "\n" + game.startsAtLabel() + "\nСтатус: " + game.status() + seats;
      if ("published".equals(game.status()) || "pending".equals(game.status())) {
        telegram.sendMessage(chatId, text, keyboard(List.of(
            row(button("Изменить", "edit_game:" + game.id())),
            row(button("Отменить игру", "cancel_game:" + game.id()))
        )));
      } else {
        telegram.sendMessage(chatId, text);
      }
    }
  }

  private void cancelGame(long chatId, long userId, String gameId) {
    var master = requireActiveMaster(chatId, userId);
    if (master == null) return;
    var result = backend.setMasterGameStatus(master.id(), gameId, "cancelled");
    telegram.sendMessage(chatId, "Игра отменена и больше не показывается в активной афише.\n\n" + result.game().title(), mainMenu());
  }

  private void showEditFields(long chatId, long userId, String gameId) {
    if (requireActiveMaster(chatId, userId) == null) return;
    telegram.sendMessage(chatId, "Что изменить?", editFieldsKeyboard(gameId));
  }

  private void beginEditField(long chatId, long userId, String data) {
    var parts = data.split(":", 3);
    if (parts.length != 3) return;
    if (requireActiveMaster(chatId, userId) == null) return;
    sessions.save(userId, "edit:" + parts[1] + ":" + parts[2], new GameDraft());
    telegram.sendMessage(chatId, editPrompt(parts[2]), cancelKeyboard());
  }

  private void updateGameField(long chatId, long userId, String state, String text) {
    var parts = state.split(":", 3);
    if (parts.length != 3) return;
    var master = requireActiveMaster(chatId, userId);
    if (master == null) return;
    var result = backend.updateMasterGame(master.id(), parts[1], updateRequest(parts[2], text));
    sessions.reset(userId);
    telegram.sendMessage(chatId, "Игра обновлена.\n\n" + result.game().title() + "\n" + result.game().startsAtLabel(), mainMenu());
  }

  private String gamePreview(GameDraft draft, String profileContactUrl) {
    return String.join("\n",
        "Проверьте карточку игры:",
        "",
        "Название: " + draft.title(),
        "Дата и время: " + draft.date() + " " + draft.time(),
        "Продолжительность: " + (draft.durationMinutes() == null ? "не указана" : formatDuration(draft.durationMinutes())),
        "Описание: " + draft.description(),
        "Система: " + draft.gameSystem(),
        "Уровень: " + draft.experienceLevel(),
        "Возраст: " + draft.ageRating(),
        "Игроки: " + draft.minPlayers() + "-" + draft.maxPlayers(),
        "Стоимость: " + draft.price() + " " + draft.currency(),
        "Контакт: " + (draft.contactOverride() == null ? profileContactUrl : draft.contactOverride())
    );
  }

  private BackendGameUpdateRequest updateRequest(String field, String text) {
    return switch (field) {
      case "title" -> new BackendGameUpdateRequest(BotTextParser.title(text), null, null, null, null, null, null, null, null, null, null, null, null);
      case "description" -> new BackendGameUpdateRequest(null, BotTextParser.description(text), null, null, null, null, null, null, null, null, null, null, null);
      case "system" -> new BackendGameUpdateRequest(null, null, BotTextParser.shortText(text, "Игровая система", 2, 80), null, null, null, null, null, null, null, null, null, null);
      case "experience" -> new BackendGameUpdateRequest(null, null, null, BotTextParser.shortText(text, "Уровень игроков", 2, 80), null, null, null, null, null, null, null, null, null);
      case "age" -> new BackendGameUpdateRequest(null, null, null, null, BotTextParser.shortText(text, "Возрастное ограничение", 2, 30), null, null, null, null, null, null, null, null);
      case "datetime" -> dateTimeUpdate(text);
      case "duration" -> new BackendGameUpdateRequest(null, null, null, null, null, null, null, BotTextParser.durationMinutes(text), null, null, null, null, null);
      case "players" -> playersUpdate(text);
      case "price" -> priceUpdate(text);
      case "contact" -> new BackendGameUpdateRequest(null, null, null, null, null, null, null, null, null, null, null, null, BotTextParser.contact(text));
      default -> throw new IllegalArgumentException("Неизвестное поле для изменения.");
    };
  }

  private BackendGameUpdateRequest dateTimeUpdate(String text) {
    var parts = BotTextParser.clean(text).split("\\s+", 2);
    if (parts.length != 2) throw new IllegalArgumentException("Введите дату и время в формате YYYY-MM-DD HH:MM.");
    BotTextParser.validateDateTime(parts[0], parts[1]);
    return new BackendGameUpdateRequest(null, null, null, null, null, parts[0], parts[1], null, null, null, null, null, null);
  }

  private BackendGameUpdateRequest playersUpdate(String text) {
    var players = BotTextParser.players(text);
    return new BackendGameUpdateRequest(null, null, null, null, null, null, null, null, players.minPlayers(), players.maxPlayers(), null, null, null);
  }

  private BackendGameUpdateRequest priceUpdate(String text) {
    var price = BotTextParser.price(text);
    return new BackendGameUpdateRequest(null, null, null, null, null, null, null, null, null, null, price.amount(), price.currency(), null);
  }

  private Object startKeyboard() {
    return keyboard(List.of(row(button("Зарегистрироваться как мастер", "register"))));
  }

  private Object mainMenu() {
    return keyboard(List.of(
        row(button("Создать игру", "create_game")),
        row(button("Мои игры", "my_games")),
        row(button("Отмена", "cancel"))
    ));
  }

  private Object confirmCancelKeyboard(String gameId) {
    return keyboard(List.of(
        row(button("Да, отменить", "confirm_cancel:" + gameId)),
        row(button("Нет, оставить", "my_games"))
    ));
  }

  private Object editFieldsKeyboard(String gameId) {
    return keyboard(List.of(
        row(button("Название", "edit_field:" + gameId + ":title"), button("Дата и время", "edit_field:" + gameId + ":datetime")),
        row(button("Описание", "edit_field:" + gameId + ":description"), button("Игроки", "edit_field:" + gameId + ":players")),
        row(button("Стоимость", "edit_field:" + gameId + ":price"), button("Система", "edit_field:" + gameId + ":system")),
        row(button("Уровень", "edit_field:" + gameId + ":experience"), button("Возраст", "edit_field:" + gameId + ":age")),
        row(button("Контакт", "edit_field:" + gameId + ":contact"), button("Длительность", "edit_field:" + gameId + ":duration")),
        row(button("Назад", "my_games"))
    ));
  }

  private String editPrompt(String field) {
    return switch (field) {
      case "title" -> "Введите новое название игры.";
      case "datetime" -> "Введите новую дату и время в формате YYYY-MM-DD HH:MM.";
      case "description" -> "Введите новое описание игры.";
      case "players" -> "Введите новое количество игроков или диапазон, например 3-5.";
      case "price" -> "Введите новую стоимость, например 35 BYN.";
      case "system" -> "Введите новую игровую систему.";
      case "experience" -> "Введите новый уровень игроков.";
      case "age" -> "Введите новое возрастное ограничение.";
      case "contact" -> "Введите новый контакт для записи: @username или https://t.me/username.";
      case "duration" -> "Введите новую длительность в часах, например 3.5.";
      default -> "Введите новое значение.";
    };
  }

  private Object cancelKeyboard() {
    return keyboard(List.of(row(button("Отмена", "cancel"))));
  }

  private Object publishKeyboard() {
    return keyboard(List.of(
        row(button("Опубликовать", "confirm_game")),
        row(button("Отмена", "cancel"))
    ));
  }

  private Object contactChoiceKeyboard() {
    return keyboard(List.of(
        row(button("Использовать контакт профиля", "use_profile_contact")),
        row(button("Указать другой", "manual_game_contact")),
        row(button("Отмена", "cancel"))
    ));
  }

  private Object durationKeyboard() {
    return keyboard(List.of(
        row(button("2 часа", "duration:120"), button("3 часа", "duration:180")),
        row(button("4 часа", "duration:240"), button("5 часов", "duration:300")),
        row(button("Пропустить", "duration:0"), button("Другое", "duration_manual")),
        row(button("Отмена", "cancel"))
    ));
  }

  private Object dateKeyboard() {
    var rows = new ArrayList<List<Map<String, String>>>();
    var today = LocalDate.now();
    for (var i = 0; i < 7; i += 1) {
      var date = today.plusDays(i);
      rows.add(row(button(date.toString(), "date:" + date)));
    }
    rows.add(row(button("Ввести дату вручную", "date_manual")));
    rows.add(row(button("Отмена", "cancel")));
    return keyboard(rows);
  }

  private Object optionKeyboard(String prefix, List<String> values) {
    var rows = new ArrayList<List<Map<String, String>>>();
    for (var value : values) {
      rows.add(row(button(value, prefix + ":" + value)));
    }
    rows.add(row(button("Ввести свой вариант", prefix + ":manual")));
    rows.add(row(button("Отмена", "cancel")));
    return keyboard(rows);
  }

  private Map<String, Object> keyboard(List<List<Map<String, String>>> rows) {
    return Map.of("inline_keyboard", rows);
  }

  @SafeVarargs
  private final List<Map<String, String>> row(Map<String, String>... buttons) {
    return List.of(buttons);
  }

  private Map<String, String> button(String text, String callbackData) {
    return Map.of("text", text, "callback_data", callbackData);
  }

  private String username(JsonNode from) {
    var value = from.path("username").asText("");
    return value.isBlank() ? null : value;
  }

  private String formatDuration(int minutes) {
    if (minutes % 60 == 0) return (minutes / 60) + " ч.";
    return minutes + " мин.";
  }

  private void sleepAfterError() {
    try {
      Thread.sleep(3_000);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
