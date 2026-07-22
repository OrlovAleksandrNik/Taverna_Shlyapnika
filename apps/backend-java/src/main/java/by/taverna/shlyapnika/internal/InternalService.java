package by.taverna.shlyapnika.internal;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.common.NotFoundException;
import by.taverna.shlyapnika.config.TavernaProperties;
import by.taverna.shlyapnika.gallery.GalleryTextFormatter;
import by.taverna.shlyapnika.gallery.domain.GalleryMediaEntity;
import by.taverna.shlyapnika.gallery.domain.GalleryPostEntity;
import by.taverna.shlyapnika.gallery.infrastructure.GalleryPostRepository;
import by.taverna.shlyapnika.internal.api.InternalBotSessionRequest;
import by.taverna.shlyapnika.internal.api.InternalBotSessionResponse;
import by.taverna.shlyapnika.internal.api.InternalGalleryPostRequest;
import by.taverna.shlyapnika.internal.api.InternalGalleryResponses.InternalGalleryPostDto;
import by.taverna.shlyapnika.internal.api.InternalMediaResponses.StoredMediaDto;
import by.taverna.shlyapnika.internal.api.InternalGameRequest;
import by.taverna.shlyapnika.internal.api.InternalGameUpdateRequest;
import by.taverna.shlyapnika.internal.api.InternalMasterRequest;
import by.taverna.shlyapnika.internal.api.InternalMasterResponse;
import by.taverna.shlyapnika.master.domain.MasterEntity;
import by.taverna.shlyapnika.master.infrastructure.MasterRepository;
import by.taverna.shlyapnika.media.MediaStorage;
import by.taverna.shlyapnika.media.MediaUpload;
import by.taverna.shlyapnika.rating.RatingService;
import by.taverna.shlyapnika.rating.api.RatingResponses.InternalRatingEventDto;
import by.taverna.shlyapnika.rating.api.RatingResponses.InternalRatingPlayerDto;
import by.taverna.shlyapnika.schedule.ScheduleService;
import by.taverna.shlyapnika.schedule.api.GameResponses.PublicGameDto;
import by.taverna.shlyapnika.schedule.domain.GameEntity;
import by.taverna.shlyapnika.schedule.infrastructure.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalService {
  private static final Set<String> GAME_STATUSES = Set.of("draft", "pending", "published", "completed", "cancelled", "archived");
  private static final Set<String> GALLERY_TYPES = Set.of("photo", "story", "character_sheet");
  private static final Set<String> GALLERY_CATEGORIES = Set.of("games", "events", "heroes", "tavern", "miniatures", "other");
  private static final Set<String> GALLERY_STATUSES = Set.of("draft", "published", "hidden");

  private final MasterRepository masters;
  private final GameRepository games;
  private final GalleryPostRepository galleryPosts;
  private final MediaStorage mediaStorage;
  private final RatingService ratingService;
  private final ScheduleService schedule;
  private final TavernaProperties properties;
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public InternalService(
      MasterRepository masters,
      GameRepository games,
      GalleryPostRepository galleryPosts,
      MediaStorage mediaStorage,
      RatingService ratingService,
      ScheduleService schedule,
      TavernaProperties properties,
      JdbcTemplate jdbcTemplate,
      AuditService auditService,
      ObjectMapper objectMapper
  ) {
    this.masters = masters;
    this.games = games;
    this.galleryPosts = galleryPosts;
    this.mediaStorage = mediaStorage;
    this.ratingService = ratingService;
    this.schedule = schedule;
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public InternalMasterResponse getMasterByTelegram(Long telegramUserId) {
    return getMasterByTelegram(telegramUserId, null);
  }

  @Transactional
  public InternalMasterResponse getMasterByTelegram(Long telegramUserId, String telegramUsername) {
    return masters.findByTelegramUserId(telegramUserId)
        .map(master -> {
          updateTelegramUsername(master, telegramUsername);
          ensureAdminRole(master);
          return toMasterResponse(master);
        })
        .orElseThrow(() -> new NotFoundException("Мастер не найден."));
  }

  @Transactional
  public InternalMasterResponse upsertMaster(InternalMasterRequest request) {
    var telegramUsername = trimToNull(request.telegramUsername());
    var displayName = request.displayName().trim();
    var contactUrl = request.contactUrl().trim();
    var master = masters.findByTelegramUserId(request.telegramUserId())
        .map(existing -> {
          existing.updateProfile(telegramUsername, displayName, contactUrl);
          return existing;
        })
        .orElseGet(() -> MasterEntity.create(request.telegramUserId(), telegramUsername, displayName, contactUrl));
    ensureAdminRole(master);
    master = masters.save(master);
    auditService.write(String.valueOf(request.telegramUserId()), "master.upserted", "Master", master.getId(), null);
    return toMasterResponse(master);
  }

  @Transactional
  public InternalMasterResponse grantAdminByTelegram(Long telegramUserId, String telegramUsername) {
    var master = masters.findByTelegramUserId(telegramUserId)
        .orElseThrow(() -> new NotFoundException("Мастер не найден."));
    updateTelegramUsername(master, telegramUsername);
    master.grantAdminRole();
    master = masters.save(master);
    auditService.write(String.valueOf(telegramUserId), "master.admin_granted", "Master", master.getId(), null);
    return toMasterResponse(master);
  }

  @Transactional(readOnly = true)
  public InternalBotSessionResponse getBotSession(Long telegramUserId) {
    var rows = jdbcTemplate.query(
        "select \"telegramUserId\", \"state\", \"draft\"::text as \"draft\" from \"BotSession\" where \"telegramUserId\" = ?",
        (rs, row) -> new InternalBotSessionResponse(
            rs.getLong("telegramUserId"),
            rs.getString("state"),
            parseDraft(rs.getString("draft"))
        ),
        telegramUserId
    );
    return rows.stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Сессия бота не найдена."));
  }

  @Transactional
  public InternalBotSessionResponse saveBotSession(Long telegramUserId, InternalBotSessionRequest request) {
    var draft = request.draft() == null || request.draft().isNull() ? objectMapper.createObjectNode() : request.draft();
    jdbcTemplate.update(
        """
        insert into "BotSession" ("telegramUserId", "state", "draft", "createdAt", "updatedAt")
        values (?, ?, ?::jsonb, current_timestamp, current_timestamp)
        on conflict ("telegramUserId") do update
        set "state" = excluded."state",
            "draft" = excluded."draft",
            "updatedAt" = current_timestamp
        """,
        telegramUserId,
        request.state(),
        draft.toString()
    );
    return new InternalBotSessionResponse(telegramUserId, request.state(), draft);
  }

  @Transactional
  public void deleteBotSession(Long telegramUserId) {
    jdbcTemplate.update("delete from \"BotSession\" where \"telegramUserId\" = ?", telegramUserId);
  }

  @Transactional
  public PublicGameDto createGame(InternalGameRequest request) {
    if (request.minPlayers() > request.maxPlayers()) {
      throw new IllegalArgumentException("Минимум игроков не может быть больше максимума.");
    }
    var master = masters.findById(request.masterId()).orElseThrow(() -> new NotFoundException("Мастер не найден."));
    var zone = ZoneId.of(properties.timezone());
    var start = LocalDate.parse(request.date()).atTime(LocalTime.parse(request.time())).atZone(zone).toInstant();
    var game = games.save(GameEntity.create(
        master,
        request.title().trim(),
        request.description().trim(),
        request.gameSystem().trim(),
        request.experienceLevel().trim(),
        request.ageRating().trim(),
        start,
        request.durationMinutes(),
        request.minPlayers(),
        request.maxPlayers(),
        request.price(),
        request.currency(),
        request.contactUrl().trim(),
        properties.autoPublish()
    ));
    auditService.write(String.valueOf(master.getTelegramUserId()), "game.created", "Game", game.getId(), "{\"status\":\"" + game.getStatus() + "\"}");
    return schedule.toDto(game);
  }

  @Transactional
  public PublicGameDto setGameStatus(String id, String status) {
    if (!GAME_STATUSES.contains(status)) throw new IllegalArgumentException("Недопустимый статус игры.");
    var game = games.findById(id).orElseThrow(() -> new NotFoundException("Игра не найдена."));
    game.setStatus(status);
    games.save(game);
    auditService.write(null, "game.status_" + status, "Game", id, null);
    return schedule.toDto(game);
  }

  @Transactional(readOnly = true)
  public java.util.List<PublicGameDto> listMasterGames(String masterId, String scope) {
    var now = Instant.now();
    return games.findByMasterIdForBot(masterId).stream()
        .filter(game -> switch (scope == null ? "all" : scope) {
          case "upcoming" -> Set.of("published", "pending").contains(game.getStatus()) && !game.getDateTimeStart().isBefore(now);
          case "past" -> game.getDateTimeStart().isBefore(now) || Set.of("completed", "cancelled", "archived").contains(game.getStatus());
          default -> true;
        })
        .limit(20)
        .map(schedule::toDto)
        .toList();
  }

  @Transactional
  public PublicGameDto setMasterGameStatus(String masterId, String gameId, String status) {
    if (!GAME_STATUSES.contains(status)) throw new IllegalArgumentException("Недопустимый статус игры.");
    var game = games.findByIdAndMasterIdForBot(gameId, masterId)
        .orElseThrow(() -> new NotFoundException("Игра не найдена или принадлежит другому мастеру."));
    game.setStatus(status);
    games.save(game);
    auditService.write(String.valueOf(game.getMaster().getTelegramUserId()), "game.status_" + status, "Game", gameId, null);
    return schedule.toDto(game);
  }

  @Transactional
  public PublicGameDto updateMasterGame(String masterId, String gameId, InternalGameUpdateRequest request) {
    var game = games.findByIdAndMasterIdForBot(gameId, masterId)
        .orElseThrow(() -> new NotFoundException("Игра не найдена или принадлежит другому мастеру."));

    if (request.title() != null) game.updateTitle(request.title().trim());
    if (request.description() != null) game.updateDescription(request.description().trim());
    if (request.gameSystem() != null) game.updateGameSystem(request.gameSystem().trim());
    if (request.experienceLevel() != null) game.updateExperienceLevel(request.experienceLevel().trim());
    if (request.ageRating() != null) game.updateAgeRating(request.ageRating().trim());
    if (request.date() != null || request.time() != null) {
      if (request.date() == null || request.time() == null) throw new IllegalArgumentException("Для изменения времени укажите дату и время.");
      var start = LocalDate.parse(request.date()).atTime(LocalTime.parse(request.time())).atZone(ZoneId.of(properties.timezone())).toInstant();
      game.updateDateTimeStart(start);
    }
    if (request.durationMinutes() != null) game.updateDurationMinutes(request.durationMinutes());
    if (request.minPlayers() != null || request.maxPlayers() != null) {
      if (request.minPlayers() == null || request.maxPlayers() == null) throw new IllegalArgumentException("Для изменения игроков укажите минимум и максимум.");
      if (request.minPlayers() > request.maxPlayers()) throw new IllegalArgumentException("Минимум игроков не может быть больше максимума.");
      game.updatePlayers(request.minPlayers(), request.maxPlayers());
    }
    if (request.price() != null) game.updatePrice(request.price(), request.currency());
    if (request.contactUrl() != null) game.updateContactUrl(request.contactUrl().trim());

    games.save(game);
    auditService.write(String.valueOf(game.getMaster().getTelegramUserId()), "game.updated", "Game", gameId, null);
    return schedule.toDto(game);
  }

  @Transactional(readOnly = true)
  public java.util.List<InternalGalleryPostDto> listMasterGalleryPosts(String masterId) {
    var master = requireMaster(masterId);
    var posts = isAdmin(master)
        ? galleryPosts.findTop12ByOrderByCreatedAtDesc()
        : galleryPosts.findTop12ByAuthorMaster_IdOrderByCreatedAtDesc(masterId);
    return posts.stream().map(this::toInternalGalleryPost).toList();
  }

  @Transactional
  public InternalGalleryPostDto createMasterGalleryPost(String masterId, InternalGalleryPostRequest request) {
    var master = requireMaster(masterId);
    var type = allowed(request.type(), GALLERY_TYPES, "Неизвестный тип публикации.");
    var category = allowed(request.category(), GALLERY_CATEGORIES, "Неизвестная категория галереи.");
    var status = allowed(request.status(), GALLERY_STATUSES, "Неизвестный статус публикации.");
    var media = request.media() == null ? java.util.List.<GalleryMediaEntity>of() : request.media().stream()
        .map(item -> GalleryMediaEntity.create(
            item.fileUrl().trim(),
            item.thumbnailUrl().trim(),
            item.mediumUrl().trim(),
            item.width(),
            item.height(),
            item.mimeType().trim(),
            trimToNull(item.altText()),
            item.sortOrder()
        ))
        .toList();
    if ("photo".equals(type) && media.isEmpty()) throw new IllegalArgumentException("Для фотопубликации добавьте хотя бы одно изображение.");

    var eventDate = request.eventDate() == null || request.eventDate().isBlank()
        ? null
        : LocalDate.parse(request.eventDate()).atStartOfDay(ZoneId.of(properties.timezone())).toInstant();
    var storyContent = trimToNull(request.storyContent());
    var post = GalleryPostEntity.create(
        Ids.newId("gal"),
        type,
        request.title().trim(),
        trimToNull(request.description()),
        storyContent,
        GalleryTextFormatter.formatStory(storyContent),
        category,
        eventDate,
        master,
        status,
        media
    );
    post = galleryPosts.save(post);
    auditService.write(String.valueOf(master.getTelegramUserId()), "gallery.post_created", "GalleryPost", post.getId(), "{\"status\":\"" + status + "\"}");
    return toInternalGalleryPost(post);
  }

  @Transactional
  public InternalGalleryPostDto setMasterGalleryPostStatus(String masterId, String postId, String status) {
    var master = requireMaster(masterId);
    var nextStatus = allowed(status, GALLERY_STATUSES, "Неизвестный статус публикации.");
    var post = isAdmin(master)
        ? galleryPosts.findById(postId)
        : galleryPosts.findByIdAndAuthorMaster_Id(postId, masterId);
    var entity = post.orElseThrow(() -> new NotFoundException("Публикация не найдена или принадлежит другому мастеру."));
    entity.setStatus(nextStatus);
    galleryPosts.save(entity);
    auditService.write(String.valueOf(master.getTelegramUserId()), "gallery.post_" + nextStatus, "GalleryPost", postId, null);
    return toInternalGalleryPost(entity);
  }

  public StoredMediaDto storeGalleryMedia(String namespace, String altText, String originalFilename, String contentType, byte[] bytes) {
    var safeNamespace = namespace == null || namespace.isBlank() ? "gallery" : namespace.trim();
    if (!safeNamespace.startsWith("gallery")) safeNamespace = "gallery/" + safeNamespace;
    var stored = mediaStorage.store(new MediaUpload(bytes, originalFilename, contentType, safeNamespace, trimToNull(altText)));
    return new StoredMediaDto(
        stored.storageKey(),
        stored.originalUrl(),
        stored.mediumUrl(),
        stored.thumbnailUrl(),
        stored.mimeType(),
        stored.width(),
        stored.height(),
        stored.sizeBytes(),
        stored.altText()
    );
  }

  @Transactional(readOnly = true)
  public java.util.List<InternalRatingPlayerDto> listRatingPlayers(String masterId, boolean includeHidden) {
    requireAdminMaster(masterId);
    return ratingService.listPlayersForBot(includeHidden);
  }

  @Transactional(readOnly = true)
  public java.util.List<InternalRatingEventDto> listRatingHistory(String masterId, Integer limit) {
    requireAdminMaster(masterId);
    return ratingService.listHistoryForBot(limit == null ? 10 : limit);
  }

  public InternalRatingPlayerDto createRatingPlayer(String masterId, String displayName, String nickname, String avatarUrl, Long telegramUserId, String idempotencyKey) {
    requireAdminMaster(masterId);
    return ratingService.createPlayer(displayName, nickname, avatarUrl, telegramUserId, masterId, idempotencyKey);
  }

  public RatingService.MutationResult addRatingGameResult(String masterId, String playerId, Integer points, String gameTitle, String gameDate, String masterName, String reason, Long telegramUserId, String idempotencyKey) {
    requireAdminMaster(masterId);
    var parsedDate = gameDate == null || gameDate.isBlank()
        ? null
        : LocalDate.parse(gameDate).atStartOfDay(ZoneId.of(properties.timezone())).toInstant();
    return ratingService.addGameResult(playerId, points, gameTitle, parsedDate, masterName, reason, telegramUserId, masterId, idempotencyKey);
  }

  public RatingService.MutationResult adjustRatingPoints(String masterId, String playerId, Integer pointsDelta, String reason, Long telegramUserId, String idempotencyKey) {
    requireAdminMaster(masterId);
    return ratingService.adjustPoints(playerId, pointsDelta, reason, telegramUserId, masterId, idempotencyKey);
  }

  public RatingService.MutationResult adjustRatingInspiration(String masterId, String playerId, Integer inspirationDelta, String reason, Long telegramUserId, String idempotencyKey) {
    requireAdminMaster(masterId);
    return ratingService.adjustInspiration(playerId, inspirationDelta, reason, telegramUserId, masterId, idempotencyKey);
  }

  public RatingService.MutationResult setRatingPlayerVisibility(String masterId, String playerId, boolean isVisible, String reason, Long telegramUserId, String idempotencyKey) {
    requireAdminMaster(masterId);
    return ratingService.setPlayerVisibility(playerId, isVisible, reason, telegramUserId, masterId, idempotencyKey);
  }

  public int archivePastGames() {
    return schedule.archivePastGames();
  }

  @Transactional
  public int withdrawConsent(String entityType, String requestId, boolean anonymize) {
    var table = switch (entityType) {
      case "GameSignup" -> "\"GameSignup\"";
      case "ServiceRequest" -> "\"ServiceRequest\"";
      case "ContactRequest" -> "\"ContactRequest\"";
      default -> throw new IllegalArgumentException("Неизвестный тип заявки.");
    };
    var updated = jdbcTemplate.update(
        "update " + table + " set \"consentWithdrawnAt\" = current_timestamp, \"dataUseStoppedAt\" = current_timestamp, \"updatedAt\" = current_timestamp where \"id\" = ?",
        requestId
    );
    if (anonymize && updated > 0) {
      jdbcTemplate.update(
          "update " + table + " set \"name\" = '[удалено]', \"contact\" = '[удалено]', \"comment\" = null, \"anonymizedAt\" = current_timestamp where \"id\" = ?",
          requestId
      );
    }
    auditService.write(null, "privacy.withdraw_consent", entityType, requestId, "{\"anonymize\":" + anonymize + "}");
    return updated;
  }

  private InternalMasterResponse toMasterResponse(MasterEntity master) {
    return new InternalMasterResponse(
        master.getId(),
        master.getTelegramUserId(),
        master.getTelegramUsername(),
        master.getDisplayName(),
        master.getContactUrl(),
        master.getRole(),
        master.getStatus()
    );
  }

  private InternalGalleryPostDto toInternalGalleryPost(GalleryPostEntity post) {
    return new InternalGalleryPostDto(
        post.getId(),
        post.getPublicId(),
        post.getType(),
        post.getTitle(),
        post.getDescription(),
        post.getCategory(),
        post.getEventDate(),
        post.getStatus(),
        post.getMedia().size(),
        post.getCreatedAt(),
        post.getPublishedAt()
    );
  }

  private MasterEntity requireMaster(String masterId) {
    return masters.findById(masterId)
        .orElseThrow(() -> new NotFoundException("Мастер не найден."));
  }

  private boolean isAdmin(MasterEntity master) {
    return "admin".equals(master.getRole());
  }

  private void ensureAdminRole(MasterEntity master) {
    if (!"admin".equals(master.getRole()) && shouldBeAdmin(master)) {
      master.grantAdminRole();
    }
  }

  private void updateTelegramUsername(MasterEntity master, String telegramUsername) {
    var username = trimToNull(telegramUsername);
    if (username == null || username.equals(master.getTelegramUsername())) return;
    master.updateProfile(username, master.getDisplayName(), master.getContactUrl());
  }

  private boolean shouldBeAdmin(MasterEntity master) {
    if (master == null || master.getTelegramUserId() == null) return false;
    var telegram = properties.telegram();
    var adminIds = telegram == null ? null : telegram.adminIds();
    if (adminIds != null && !adminIds.isBlank()) {
      var idMatches = Arrays.stream(adminIds.split(","))
          .map(String::trim)
          .filter(value -> !value.isBlank())
          .anyMatch(value -> value.equals(String.valueOf(master.getTelegramUserId())));
      if (idMatches) return true;
    }
    var adminUsernames = telegram == null ? null : telegram.adminUsernames();
    if (adminUsernames != null && !adminUsernames.isBlank()) {
      var usernameMatches = Arrays.stream(adminUsernames.split(","))
          .map(String::trim)
          .filter(value -> !value.isBlank())
          .anyMatch(master::hasTelegramUsername);
      if (usernameMatches) return true;
    }
    return masters.countAdmins() == 0 && masters.findActiveMasters().size() == 1;
  }

  private MasterEntity requireAdminMaster(String masterId) {
    var master = requireMaster(masterId);
    if (!"active".equals(master.getStatus()) || !isAdmin(master)) throw new IllegalArgumentException("Управление рейтингом доступно только администраторам Таверны.");
    return master;
  }

  private String allowed(String value, Set<String> allowed, String message) {
    var normalized = value == null ? "" : value.trim();
    if (!allowed.contains(normalized)) throw new IllegalArgumentException(message);
    return normalized;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private JsonNode parseDraft(String value) {
    try {
      return value == null || value.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(value);
    } catch (Exception error) {
      return objectMapper.createObjectNode();
    }
  }
}
