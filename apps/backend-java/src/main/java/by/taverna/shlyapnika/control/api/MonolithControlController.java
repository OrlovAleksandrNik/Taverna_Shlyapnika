package by.taverna.shlyapnika.control.api;

import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.common.Ids;
import by.taverna.shlyapnika.config.TavernaProperties;
import by.taverna.shlyapnika.media.MediaStorage;
import by.taverna.shlyapnika.media.MediaUpload;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MonolithControlController {
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;
  private final TavernaProperties properties;
  private final MediaStorage mediaStorage;

  public MonolithControlController(JdbcTemplate jdbcTemplate, AuditService auditService, TavernaProperties properties, MediaStorage mediaStorage) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
    this.properties = properties;
    this.mediaStorage = mediaStorage;
  }

  @GetMapping("/api/v1/admin/dashboard")
  public DashboardResponse dashboard() {
    // Кабинет пока читает только безопасные сводки, без контактов игроков и клиентов.
    var metrics = List.of(
        new MetricDto("Ближайшие игры", count("""
            select count(*) from "Game"
            where "dateTimeStart" >= current_timestamp
              and "status" not in ('cancelled', 'archived', 'completed')
            """), "основная база"),
        new MetricDto("Новые заявки", count("""
            select count(*) from "ServiceRequest"
            where "status" = 'new'
            """), "без контактов"),
        new MetricDto("Активные мастера", count("""
            select count(*) from "Master"
            where "status" = 'active'
            """), "профили"),
        new MetricDto("Публикации галереи", count("""
            select count(*) from "GalleryPost"
            where "status" = 'published'
              and "isVisible" = true
            """), "галерея"),
        new MetricDto("Игроки рейтинга", count("""
            select count(*) from "RatingPlayer"
            where "isVisible" = true
            """), "рейтинг"),
        new MetricDto("Записи на игры", count("""
            select count(*) from "GameSignup"
            where "status" = 'confirmed'
            """), "подтверждены")
    );
    return new DashboardResponse(
        "monolith",
        Instant.now(),
        metrics,
        upcomingGames(8),
        recentActions(8)
    );
  }

  @GetMapping("/api/v1/admin/games")
  public ItemsResponse<GameRowDto> games(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return new ItemsResponse<>(gameRows(false, page, size), page, size);
  }

  @PostMapping("/api/v1/admin/games")
  @ResponseStatus(HttpStatus.CREATED)
  public GameRowDto createGame(@RequestBody AdminGameRequest request) {
    var master = findMaster(request.masterPublicId());
    var id = Ids.newId("gm");
    var startsAt = Instant.parse(required(request.startsAt(), "startsAt"));
    var durationMinutes = positiveOrDefault(request.durationMinutes(), 180);
    var endsAt = startsAt.plusSeconds(durationMinutes.longValue() * 60);
    var status = properties.autoPublish() ? "published" : "draft";
    jdbcTemplate.update("""
        insert into "Game" (
          "id", "masterId", "title", "description", "gameSystem", "experienceLevel", "ageRating",
          "dateTimeStart", "durationMinutes", "dateTimeEnd", "minPlayers", "maxPlayers",
          "price", "currency", "imageUrl", "contactUrl", "status", "publishedAt", "createdAt", "updatedAt"
        )
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null, ?, ?::"GameStatus",
                case when ? = 'published' then current_timestamp else null end,
                current_timestamp, current_timestamp)
        """,
        id,
        master.id(),
        textOrDefault(request.title(), "Новая игра"),
        textOrDefault(request.description(), "Описание будет добавлено мастером."),
        textOrDefault(request.gameSystem(), "D&D 5e"),
        textOrDefault(request.experienceLevel(), "любой уровень"),
        textOrDefault(request.ageRating(), "12+"),
        Timestamp.from(startsAt),
        durationMinutes,
        Timestamp.from(endsAt),
        positiveOrDefault(request.minPlayers(), 1),
        positiveOrDefault(request.maxPlayers(), 5),
        request.price() == null ? BigDecimal.ZERO : request.price(),
        textOrDefault(request.currency(), "BYN"),
        textOrDefault(request.contactUrl(), master.contactUrl()),
        status,
        status
    );
    auditService.write("master-cabinet", "game.created_from_cabinet", "Game", id, "{\"status\":\"" + status + "\"}");
    return new GameRowDto(id, textOrDefault(request.title(), "Новая игра"), startsAt, master.id(), master.displayName(), textOrDefault(request.gameSystem(), "D&D 5e"), status);
  }

  @PutMapping("/api/v1/admin/games/{id}")
  public GameRowDto updateGame(@PathVariable String id, @RequestBody AdminGameRequest request) {
    var master = trimToNull(request.masterPublicId()) == null ? null : findMaster(request.masterPublicId());
    var startsAt = trimToNull(request.startsAt()) == null ? null : Instant.parse(request.startsAt().trim());
    jdbcTemplate.update("""
        update "Game"
        set "title" = coalesce(nullif(?, ''), "title"),
            "description" = coalesce(nullif(?, ''), "description"),
            "gameSystem" = coalesce(nullif(?, ''), "gameSystem"),
            "experienceLevel" = coalesce(nullif(?, ''), "experienceLevel"),
            "ageRating" = coalesce(nullif(?, ''), "ageRating"),
            "masterId" = coalesce(?, "masterId"),
            "dateTimeStart" = coalesce(?, "dateTimeStart"),
            "durationMinutes" = coalesce(?, "durationMinutes"),
            "minPlayers" = coalesce(?, "minPlayers"),
            "maxPlayers" = coalesce(?, "maxPlayers"),
            "price" = coalesce(?, "price"),
            "currency" = coalesce(nullif(?, ''), "currency"),
            "contactUrl" = coalesce(nullif(?, ''), "contactUrl"),
            "dateTimeEnd" = case
              when ?::timestamp is not null then ?::timestamp + (coalesce(?, "durationMinutes") * interval '1 minute')
              when ?::int is not null then "dateTimeStart" + (?::int * interval '1 minute')
              else "dateTimeEnd"
            end,
            "updatedAt" = current_timestamp
        where "id" = ?
        """,
        trimToNull(request.title()),
        trimToNull(request.description()),
        trimToNull(request.gameSystem()),
        trimToNull(request.experienceLevel()),
        trimToNull(request.ageRating()),
        master == null ? null : master.id(),
        startsAt == null ? null : Timestamp.from(startsAt),
        request.durationMinutes(),
        request.minPlayers(),
        request.maxPlayers(),
        request.price(),
        trimToNull(request.currency()),
        trimToNull(request.contactUrl()),
        startsAt == null ? null : Timestamp.from(startsAt),
        startsAt == null ? null : Timestamp.from(startsAt),
        request.durationMinutes(),
        request.durationMinutes(),
        request.durationMinutes(),
        id);
    auditService.write("master-cabinet", "game.updated_from_cabinet", "Game", id, null);
    return gameById(id);
  }

  @PostMapping("/api/v1/admin/games/{id}/publish")
  public GameRowDto publishGame(@PathVariable String id) {
    return setGameStatus(id, "published", "game.published_from_cabinet");
  }

  @PostMapping("/api/v1/admin/games/{id}/cancel")
  public GameRowDto cancelGame(@PathVariable String id) {
    return setGameStatus(id, "cancelled", "game.cancelled_from_cabinet");
  }

  @DeleteMapping("/api/v1/admin/games/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archiveGame(@PathVariable String id) {
    setGameStatus(id, "archived", "game.archived_from_cabinet");
  }

  @PostMapping("/api/v1/admin/gallery/posts/{publicId}/publish")
  public GalleryPostRowDto publishGalleryPost(@PathVariable String publicId) {
    return setGalleryPostState(publicId, "published", true, "gallery.post_published_from_cabinet");
  }

  @PostMapping("/api/v1/admin/gallery/posts/{publicId}/hide")
  public GalleryPostRowDto hideGalleryPost(@PathVariable String publicId) {
    return setGalleryPostState(publicId, "hidden", false, "gallery.post_hidden_from_cabinet");
  }

  @DeleteMapping("/api/v1/admin/gallery/posts/{publicId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteGalleryPost(@PathVariable String publicId) {
    var deleted = jdbcTemplate.update("""
        delete from "GalleryPost"
        where "publicId" = ?
        """, publicId);
    if (deleted > 0) {
      auditService.write("master-cabinet", "gallery.post_deleted_from_cabinet", "GalleryPost", publicId, null);
    }
  }

  @PostMapping("/api/v1/admin/service-requests/{id}/contact")
  public ControlRecordDto contactServiceRequest(@PathVariable String id) {
    return setServiceRequestStatus(id, "contacted", "service_request.contacted_from_cabinet");
  }

  @PostMapping("/api/v1/admin/service-requests/{id}/close")
  public ControlRecordDto closeServiceRequest(@PathVariable String id) {
    return setServiceRequestStatus(id, "closed", "service_request.closed_from_cabinet");
  }

  @PostMapping("/api/v1/admin/masters/{id}/activate")
  public ControlRecordDto activateMaster(@PathVariable String id) {
    return setMasterStatus(id, "active", "master.activated_from_cabinet");
  }

  @PostMapping("/api/v1/admin/masters/{id}/block")
  public ControlRecordDto blockMaster(@PathVariable String id) {
    return setMasterStatus(id, "blocked", "master.blocked_from_cabinet");
  }

  @GetMapping("/api/v1/admin/schedule")
  public ItemsResponse<GameRowDto> schedule(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to
  ) {
    var fromInstant = trimToNull(from) == null ? Instant.now() : Instant.parse(from);
    var toInstant = trimToNull(to) == null ? fromInstant.plusSeconds(45L * 24 * 60 * 60) : Instant.parse(to);
    return new ItemsResponse<>(gameRowsBetween(fromInstant, toInstant, 100), 0, 100);
  }

  @GetMapping("/api/v1/admin/signups/summary")
  public ItemsResponse<SignupSummaryDto> signupSummary(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var offset = safePage * safeSize;
    var items = jdbcTemplate.query("""
        select g."id" as "gameId", g."title" as "gameTitle", g."dateTimeStart", g."maxPlayers",
               m."displayName" as "masterName",
               count(s."id") filter (where s."status" = 'confirmed')::int as "confirmedSignups",
               coalesce(sum(s."seats") filter (where s."status" = 'confirmed'), 0)::int as "confirmedSeats"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        left join "GameSignup" s on s."gameId" = g."id"
        where g."dateTimeStart" >= current_timestamp
          and g."status" not in ('cancelled', 'archived', 'completed')
        group by g."id", g."title", g."dateTimeStart", g."maxPlayers", m."displayName"
        order by g."dateTimeStart" asc
        limit ? offset ?
        """, (rs, rowNum) -> new SignupSummaryDto(
            rs.getString("gameId"),
            rs.getString("gameTitle"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterName"),
            rs.getInt("confirmedSignups"),
            rs.getInt("confirmedSeats"),
            rs.getInt("maxPlayers")
        ), safeSize, offset);
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/gallery/posts")
  public ItemsResponse<GalleryPostRowDto> galleryPostRows(
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var offset = safePage * safeSize;
    var items = jdbcTemplate.query("""
        select p."publicId", p."type"::text as "type", p."title", p."category"::text as "category",
               p."status"::text as "status", p."isVisible", p."eventDate", p."publishedAt",
               p."createdAt", p."updatedAt", m."displayName" as "authorName",
               (
                 select coalesce(first_media."thumbnailUrl", first_media."mediumUrl", first_media."fileUrl")
                 from "GalleryMedia" first_media
                 where first_media."galleryPostId" = p."id"
                 order by first_media."sortOrder" asc, first_media."createdAt" asc
                 limit 1
               ) as "previewUrl",
               count(media."id")::int as "mediaCount"
        from "GalleryPost" p
        left join "Master" m on m."id" = p."authorMasterId"
        left join "GalleryMedia" media on media."galleryPostId" = p."id"
        where (?::text is null or p."type" = cast(? as "GalleryPostType"))
        group by p."publicId", p."type", p."title", p."category", p."status", p."isVisible",
                 p."eventDate", p."publishedAt", p."createdAt", p."updatedAt", m."displayName"
        order by p."createdAt" desc
        limit ? offset ?
        """, (rs, rowNum) -> new GalleryPostRowDto(
            rs.getString("publicId"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("status"),
            rs.getBoolean("isVisible"),
            rs.getInt("mediaCount"),
            rs.getString("previewUrl"),
            rs.getString("authorName"),
            instant(rs, "eventDate"),
            instant(rs, "publishedAt"),
            instant(rs, "createdAt"),
            instant(rs, "updatedAt")
        ), trimToNull(type), trimToNull(type), safeSize, offset);
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/gallery/posts/{publicId}")
  public GalleryPostDetailsDto galleryPostDetails(@PathVariable String publicId) {
    var post = jdbcTemplate.queryForObject("""
        select p."publicId", p."type"::text as "type", p."title", p."description",
               p."storyHtml", p."category"::text as "category", p."status"::text as "status",
               p."isVisible", p."eventDate", p."publishedAt", p."createdAt", p."updatedAt",
               m."displayName" as "authorName"
        from "GalleryPost" p
        left join "Master" m on m."id" = p."authorMasterId"
        where p."publicId" = ?
        """, (rs, rowNum) -> new GalleryPostDetailsDto(
            rs.getString("publicId"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("storyHtml"),
            rs.getString("category"),
            rs.getString("status"),
            rs.getBoolean("isVisible"),
            rs.getString("authorName"),
            instant(rs, "eventDate"),
            instant(rs, "publishedAt"),
            instant(rs, "createdAt"),
            instant(rs, "updatedAt"),
            List.of()
        ), publicId);
    var media = jdbcTemplate.query("""
        select "id", "fileUrl", "thumbnailUrl", "mediumUrl", "width", "height", "mimeType", "altText"
        from "GalleryMedia"
        where "galleryPostId" = (
          select "id" from "GalleryPost" where "publicId" = ?
        )
        order by "sortOrder" asc, "createdAt" asc
        """, (rs, rowNum) -> new GalleryMediaDto(
            rs.getString("id"),
            rs.getString("fileUrl"),
            rs.getString("thumbnailUrl"),
            rs.getString("mediumUrl"),
            (Integer) rs.getObject("width"),
            (Integer) rs.getObject("height"),
            rs.getString("mimeType"),
            rs.getString("altText")
        ), publicId);
    return post.withMedia(media);
  }

  @PostMapping("/api/v1/admin/rating/players/{id}/adjust")
  public RatingPlayerRowDto adjustRatingPlayer(@PathVariable String id, @RequestBody RatingAdjustmentRequest request) {
    var gamesDelta = request.gamesDelta() == null ? 0 : request.gamesDelta();
    var pointsDelta = request.pointsDelta() == null ? 0 : request.pointsDelta();
    var inspirationDelta = request.inspirationDelta() == null ? 0 : request.inspirationDelta();
    jdbcTemplate.update("""
        update "RatingPlayer"
        set "gamesPlayed" = greatest(0, "gamesPlayed" + ?),
            "totalPoints" = greatest(0, "totalPoints" + ?),
            "inspirationCount" = greatest(0, "inspirationCount" + ?),
            "lastStatsAt" = current_timestamp,
            "updatedAt" = current_timestamp
        where "id" = ?
        """, gamesDelta, pointsDelta, inspirationDelta, id);
    jdbcTemplate.update("""
        insert into "RatingEvent" (
          "id", "playerId", "type", "pointsDelta", "inspirationDelta", "gamesDelta",
          "reason", "createdByMasterId", "createdAt"
        )
        values (?, ?, 'correction'::"RatingEventType", ?, ?, ?, ?, null, current_timestamp)
        """,
        Ids.newId("rte"),
        id,
        pointsDelta,
        inspirationDelta,
        gamesDelta,
        textOrDefault(request.reason(), "Правка из кабинета мастера"));
    auditService.write("master-cabinet", "rating.player_adjusted_from_cabinet", "RatingPlayer", id,
        "{\"gamesDelta\":" + gamesDelta + ",\"pointsDelta\":" + pointsDelta + ",\"inspirationDelta\":" + inspirationDelta + "}");
    return ratingPlayerById(id);
  }

  @GetMapping("/api/v1/admin/master-access-requests")
  public ItemsResponse<MasterAccessRequestRowDto> masterAccessRequests(
      @RequestParam(defaultValue = "pending") String status
  ) {
    var safeStatus = List.of("pending", "approved", "rejected").contains(status) ? status : "pending";
    var items = jdbcTemplate.query("""
        select "id", "displayName", "email", "telegramUsername", "requestedRole",
               "status"::text as "status", "createdAt", "updatedAt"
        from "MasterAccessRequest"
        where "status" = ?::"MasterAccessRequestStatus"
        order by "createdAt" asc
        limit 100
        """, (rs, rowNum) -> new MasterAccessRequestRowDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("email"),
            rs.getString("telegramUsername"),
            rs.getString("requestedRole"),
            rs.getString("status"),
            instant(rs, "createdAt"),
            instant(rs, "updatedAt")
        ), safeStatus);
    return new ItemsResponse<>(items, 0, 100);
  }

  @PostMapping("/api/v1/admin/master-access-requests/{id}/approve")
  public MasterAccessRequestRowDto approveMasterAccessRequest(@PathVariable String id) {
    jdbcTemplate.update("""
        update "MasterAccessRequest"
        set "status" = 'approved'::"MasterAccessRequestStatus",
            "decidedAt" = current_timestamp,
            "decisionComment" = 'Одобрено из кабинета Шляпника',
            "updatedAt" = current_timestamp
        where "id" = ?
        """, id);
    auditService.write("master-cabinet", "master.access_approved_from_cabinet", "MasterAccessRequest", id, null);
    return masterAccessRequestById(id);
  }

  @PostMapping("/api/v1/admin/master-access-requests/{id}/reject")
  public MasterAccessRequestRowDto rejectMasterAccessRequest(@PathVariable String id) {
    jdbcTemplate.update("""
        update "MasterAccessRequest"
        set "status" = 'rejected'::"MasterAccessRequestStatus",
            "decidedAt" = current_timestamp,
            "decisionComment" = 'Отклонено из кабинета Шляпника',
            "updatedAt" = current_timestamp
        where "id" = ?
        """, id);
    auditService.write("master-cabinet", "master.access_rejected_from_cabinet", "MasterAccessRequest", id, null);
    return masterAccessRequestById(id);
  }

  @GetMapping("/api/v1/admin/profile")
  public MasterProfileDto profile(HttpServletRequest request) {
    return profileBySession(request);
  }

  @PutMapping("/api/v1/admin/profile")
  public MasterProfileDto updateProfile(HttpServletRequest request, @RequestBody MasterProfileRequest body) {
    var profile = profileBySession(request);
    jdbcTemplate.update("""
        update "Master"
        set "displayName" = coalesce(nullif(?, ''), "displayName"),
            "telegramUsername" = coalesce(nullif(?, ''), "telegramUsername"),
            "contactUrl" = coalesce(nullif(?, ''), "contactUrl"),
            "profilePhotoUrl" = coalesce(nullif(?, ''), "profilePhotoUrl"),
            "profileStatus" = nullif(?, ''),
            "profileBio" = nullif(?, ''),
            "profileStyle" = nullif(?, ''),
            "profileInterests" = nullif(?, ''),
            "profileSystems" = nullif(?, ''),
            "profileExperience" = nullif(?, ''),
            "phone" = nullif(?, ''),
            "extraLinks" = nullif(?, ''),
            "updatedAt" = current_timestamp
        where "id" = ?
        """,
        trimToNull(body.displayName()),
        trimToNull(body.telegramUsername()),
        trimToNull(body.contactUrl()),
        trimToNull(body.photoUrl()),
        trimToNull(body.status()),
        trimToNull(body.bio()),
        trimToNull(body.style()),
        trimToNull(body.interests()),
        trimToNull(body.systems()),
        trimToNull(body.experience()),
        trimToNull(body.phone()),
        trimToNull(body.extraLinks()),
        profile.id());
    auditService.write("master-cabinet", "master.profile_updated_from_cabinet", "Master", profile.id(), null);
    return masterProfileById(profile.id());
  }

  @PostMapping(value = "/api/v1/admin/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public MasterProfileDto uploadProfilePhoto(HttpServletRequest request, @RequestPart("file") MultipartFile file) throws Exception {
    var profile = profileBySession(request);
    var stored = mediaStorage.store(new MediaUpload(
        file.getBytes(),
        file.getOriginalFilename(),
        file.getContentType(),
        "masters",
        "Фото мастера " + profile.displayName()));
    jdbcTemplate.update("""
        update "Master"
        set "profilePhotoUrl" = ?,
            "updatedAt" = current_timestamp
        where "id" = ?
        """, stored.originalUrl(), profile.id());
    auditService.write("master-cabinet", "master.profile_photo_updated_from_cabinet", "Master", profile.id(), null);
    return masterProfileById(profile.id());
  }

  @GetMapping("/api/v1/admin/data/{section}")
  public ItemsResponse<ControlRecordDto> data(
      @PathVariable String section,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var offset = safePage * safeSize;
    var items = switch (section) {
      case "applications", "services" -> serviceRequests(safeSize, offset);
      case "masters" -> masters(safeSize, offset);
      case "players", "rating" -> ratingPlayers(safeSize, offset);
      case "gallery" -> galleryPosts(null, safeSize, offset);
      case "stories" -> galleryPosts("story", safeSize, offset);
      case "notifications" -> recentNotificationLikeActions(safeSize);
      default -> List.<ControlRecordDto>of();
    };
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/audit")
  public ItemsResponse<ActionDto> audit(@RequestParam(defaultValue = "0") int page) {
    return new ItemsResponse<>(recentActions(30), Math.max(page, 0), 30);
  }

  @GetMapping("/api/v1/admin/projects")
  public List<ProjectDto> projects() {
    return List.of(
        new ProjectDto("site-monolith", "Основной сайт и API", "Java 21 Spring Boot + static frontend", "apps/backend-java", "active", "monolith"),
        new ProjectDto("telegram-bot", "Писарь таверны", "Java Telegram Bot + internal API", "apps/telegram-bot-java", "active", "monolith"),
        new ProjectDto("master-cabinet", "Кабинет мастера", "Vite frontend inside monolith", "apps/master-cabinet", "active", "monolith")
    );
  }

  @GetMapping("/api/v1/admin/users")
  public ItemsResponse<AccountDto> users(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var items = jdbcTemplate.query("""
        select "id", "telegramUsername", "displayName", "role", "status"
        from "Master"
        order by "displayName" asc
        limit ? offset ?
        """, (rs, rowNum) -> new AccountDto(
            rs.getString("id"),
            publicTelegramHandle(rs.getString("telegramUsername")),
            List.of(rs.getString("role")),
            rs.getString("status")
        ), safeSize, safePage * safeSize);
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/rating/players")
  public ItemsResponse<RatingPlayerRowDto> ratingPlayerRows(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var offset = safePage * safeSize;
    var items = jdbcTemplate.query("""
        select "id", "displayName", "nickname", "isVisible", "gamesPlayed",
               "totalPoints", "inspirationCount",
               round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
               "lastGameAt", "lastStatsAt", "updatedAt"
        from "RatingPlayer"
        order by "totalPoints" desc,
                 case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
                 "gamesPlayed" desc,
                 lower("displayName") asc
        limit ? offset ?
        """, (rs, rowNum) -> new RatingPlayerRowDto(
            offset + rowNum + 1,
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("nickname"),
            rs.getBoolean("isVisible"),
            rs.getInt("gamesPlayed"),
            rs.getInt("totalPoints"),
            rs.getInt("inspirationCount"),
            rs.getBigDecimal("averagePointsPerGame"),
            instant(rs, "lastGameAt"),
            instant(rs, "lastStatsAt"),
            instant(rs, "updatedAt")
        ), safeSize, offset);
    return new ItemsResponse<>(items, safePage, safeSize);
  }

  @GetMapping("/api/v1/admin/files/storage")
  public StorageDto filesStorage() {
    return new StorageDto(
        new StorageAdapterDto("local", properties.fileStorageDir()),
        new StorageAdapterDto("disabled", "project artifacts are not enabled in monolith yet"),
        List.of("s3-compatible", "railway-volume")
    );
  }

  @GetMapping("/api/v1/admin/backups")
  public ItemsResponse<BackupJobDto> backups() {
    return new ItemsResponse<>(List.of(
        new BackupJobDto("backup-read-only", "DISABLED", "manual database backups required", "not configured")
    ), 0, 20);
  }

  @GetMapping("/api/v1/admin/integration/status")
  public Map<String, Object> integrationStatus() {
    return Map.of(
        "mode", "monolith",
        "mainSiteIntegrationEnabled", true,
        "telegramIntegrationEnabled", true,
        "desktopAgentEnabled", false,
        "contractsPrepared", true,
        "productionDataUsed", true
    );
  }

  @GetMapping("/api/v1/admin/settings")
  public Map<String, Object> settings() {
    return Map.of(
        "featureFlags", Map.of(
            "publicRegistration", false,
            "mainSiteIntegration", properties.serveFrontend(),
            "telegramIntegration", properties.telegram() != null && properties.telegram().botToken() != null && !properties.telegram().botToken().isBlank(),
            "desktopAgent", false,
            "autoPublishGames", properties.autoPublish()
        ),
        "storedSettings", List.of(
            new SettingDto("timezone", properties.timezone(), false, false),
            new SettingDto("siteBaseUrl", properties.siteBaseUrl(), false, false),
            new SettingDto("publicUploadsUrl", properties.publicUploadsUrl(), false, false),
            new SettingDto("frontendStaticDir", properties.frontendStaticDir(), false, false),
            new SettingDto("corsOriginsCount", String.valueOf(properties.allowedOrigins().size()), false, false),
            new SettingDto("internalApiToken", "configured", true, true)
        )
    );
  }

  private List<GameRowDto> gameRows(boolean onlyFuture, int page, int size) {
    var safePage = Math.max(page, 0);
    var safeSize = Math.min(Math.max(size, 1), 100);
    var dateFilter = onlyFuture ? "where g.\"dateTimeStart\" >= current_timestamp" : "";
    return jdbcTemplate.query("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        %s
        order by g."dateTimeStart" asc
        limit ? offset ?
        """.formatted(dateFilter), (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), safeSize, safePage * safeSize);
  }

  private List<GameRowDto> upcomingGames(int limit) {
    return jdbcTemplate.query("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        where g."dateTimeStart" >= current_timestamp
          and g."status" not in ('cancelled', 'archived', 'completed')
        order by g."dateTimeStart" asc
        limit ?
        """, (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), limit);
  }

  private List<GameRowDto> gameRowsBetween(Instant from, Instant to, int limit) {
    return jdbcTemplate.query("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        where g."dateTimeStart" >= ?
          and g."dateTimeStart" <= ?
        order by g."dateTimeStart" asc
        limit ?
        """, (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), Timestamp.from(from), Timestamp.from(to), limit);
  }

  private List<ControlRecordDto> serviceRequests(int limit, int offset) {
    return jdbcTemplate.query("""
        select "id", "service", "status", "updatedAt"
        from "ServiceRequest"
        order by "createdAt" desc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("service"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), limit, offset);
  }

  private ControlRecordDto setServiceRequestStatus(String id, String status, String action) {
    jdbcTemplate.update("""
        update "ServiceRequest"
        set "status" = ?::"ServiceRequestStatus",
            "updatedAt" = current_timestamp
        where "id" = ?
        """, status, id);
    auditService.write("master-cabinet", action, "ServiceRequest", id, "{\"status\":\"" + status + "\"}");
    return serviceRequestById(id);
  }

  private ControlRecordDto serviceRequestById(String id) {
    return jdbcTemplate.queryForObject("""
        select "id", "service", "status", "updatedAt"
        from "ServiceRequest"
        where "id" = ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("service"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), id);
  }

  private List<ControlRecordDto> masters(int limit, int offset) {
    return jdbcTemplate.query("""
        select "id", "displayName", "status", "updatedAt"
        from "Master"
        order by "displayName" asc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), limit, offset);
  }

  private ControlRecordDto setMasterStatus(String id, String status, String action) {
    jdbcTemplate.update("""
        update "Master"
        set "status" = ?::"MasterStatus",
            "updatedAt" = current_timestamp
        where "id" = ?
        """, status, id);
    auditService.write("master-cabinet", action, "Master", id, "{\"status\":\"" + status + "\"}");
    return masterById(id);
  }

  private ControlRecordDto masterById(String id) {
    return jdbcTemplate.queryForObject("""
        select "id", "displayName", "status", "updatedAt"
        from "Master"
        where "id" = ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), id);
  }

  private List<ControlRecordDto> ratingPlayers(int limit, int offset) {
    return jdbcTemplate.query("""
        select "id", "displayName", "isVisible", "updatedAt"
        from "RatingPlayer"
        order by "totalPoints" desc, "gamesPlayed" desc, lower("displayName") asc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getBoolean("isVisible") ? "published" : "hidden",
            instant(rs, "updatedAt")
        ), limit, offset);
  }

  private List<ControlRecordDto> galleryPosts(String type, int limit, int offset) {
    return jdbcTemplate.query("""
        select "publicId", "title", "status", "updatedAt"
        from "GalleryPost"
        where (?::text is null or "type" = cast(? as "GalleryPostType"))
        order by "createdAt" desc
        limit ? offset ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("publicId"),
            rs.getString("title"),
            rs.getString("status"),
            instant(rs, "updatedAt")
        ), type, type, limit, offset);
  }

  private List<ControlRecordDto> recentNotificationLikeActions(int limit) {
    return jdbcTemplate.query("""
        select "id", "action", "entityType", "createdAt"
        from "AuditLog"
        order by "createdAt" desc
        limit ?
        """, (rs, rowNum) -> new ControlRecordDto(
            rs.getString("id"),
            rs.getString("action"),
            rs.getString("entityType"),
            instant(rs, "createdAt")
        ), limit);
  }

  private List<ActionDto> recentActions(int limit) {
    return jdbcTemplate.query("""
        select "userId", "action", "entityType", "createdAt"
        from "AuditLog"
        order by "createdAt" desc
        limit ?
        """, (rs, rowNum) -> new ActionDto(
            blankToSystem(rs.getString("userId")),
            rs.getString("action"),
            rs.getString("entityType"),
            instant(rs, "createdAt")
        ), limit);
  }

  private int count(String sql) {
    var value = jdbcTemplate.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private GameRowDto setGameStatus(String id, String status, String action) {
    jdbcTemplate.update("""
        update "Game"
        set "status" = ?::"GameStatus",
            "publishedAt" = case when ? = 'published' and "publishedAt" is null then current_timestamp else "publishedAt" end,
            "cancelledAt" = case when ? = 'cancelled' then current_timestamp else "cancelledAt" end,
            "completedAt" = case when ? in ('completed', 'archived') then current_timestamp else "completedAt" end,
            "updatedAt" = current_timestamp
        where "id" = ?
        """, status, status, status, status, id);
    auditService.write("master-cabinet", action, "Game", id, "{\"status\":\"" + status + "\"}");
    return gameById(id);
  }

  private GameRowDto gameById(String id) {
    return jdbcTemplate.queryForObject("""
        select g."id", g."title", g."dateTimeStart", g."status",
               g."gameSystem", m."id" as "masterId", m."displayName" as "masterName"
        from "Game" g
        join "Master" m on m."id" = g."masterId"
        where g."id" = ?
        """, (rs, rowNum) -> new GameRowDto(
            rs.getString("id"),
            rs.getString("title"),
            instant(rs, "dateTimeStart"),
            rs.getString("masterId"),
            rs.getString("masterName"),
            rs.getString("gameSystem"),
            rs.getString("status")
        ), id);
  }

  private GalleryPostRowDto setGalleryPostState(String publicId, String status, boolean visible, String action) {
    jdbcTemplate.update("""
        update "GalleryPost"
        set "status" = ?::"GalleryPostStatus",
            "isVisible" = ?,
            "publishedAt" = case when ? = 'published' and "publishedAt" is null then current_timestamp else "publishedAt" end,
            "updatedAt" = current_timestamp
        where "publicId" = ?
        """, status, visible, status, publicId);
    auditService.write("master-cabinet", action, "GalleryPost", publicId,
        "{\"status\":\"" + status + "\",\"visible\":" + visible + "}");
    return galleryPostByPublicId(publicId);
  }

  private GalleryPostRowDto galleryPostByPublicId(String publicId) {
    return jdbcTemplate.queryForObject("""
        select p."publicId", p."type"::text as "type", p."title", p."category"::text as "category",
               p."status"::text as "status", p."isVisible", p."eventDate", p."publishedAt",
               p."createdAt", p."updatedAt", m."displayName" as "authorName",
               (
                 select coalesce(first_media."thumbnailUrl", first_media."mediumUrl", first_media."fileUrl")
                 from "GalleryMedia" first_media
                 where first_media."galleryPostId" = p."id"
                 order by first_media."sortOrder" asc, first_media."createdAt" asc
                 limit 1
               ) as "previewUrl",
               count(media."id")::int as "mediaCount"
        from "GalleryPost" p
        left join "Master" m on m."id" = p."authorMasterId"
        left join "GalleryMedia" media on media."galleryPostId" = p."id"
        where p."publicId" = ?
        group by p."publicId", p."type", p."title", p."category", p."status", p."isVisible",
                 p."eventDate", p."publishedAt", p."createdAt", p."updatedAt", m."displayName"
        """, (rs, rowNum) -> new GalleryPostRowDto(
            rs.getString("publicId"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("status"),
            rs.getBoolean("isVisible"),
            rs.getInt("mediaCount"),
            rs.getString("previewUrl"),
            rs.getString("authorName"),
            instant(rs, "eventDate"),
            instant(rs, "publishedAt"),
            instant(rs, "createdAt"),
            instant(rs, "updatedAt")
        ), publicId);
  }

  private RatingPlayerRowDto ratingPlayerById(String id) {
    return jdbcTemplate.queryForObject("""
        select ranked.*
        from (
          select row_number() over (
                   order by "totalPoints" desc,
                            case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end desc,
                            "gamesPlayed" desc,
                            lower("displayName") asc
                 )::int as "rank",
                 "id", "displayName", "nickname", "isVisible", "gamesPlayed",
                 "totalPoints", "inspirationCount",
                 round(case when "gamesPlayed" > 0 then "totalPoints"::numeric / "gamesPlayed" else 0 end, 2) as "averagePointsPerGame",
                 "lastGameAt", "lastStatsAt", "updatedAt"
          from "RatingPlayer"
        ) ranked
        where ranked."id" = ?
        """, (rs, rowNum) -> new RatingPlayerRowDto(
            rs.getInt("rank"),
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("nickname"),
            rs.getBoolean("isVisible"),
            rs.getInt("gamesPlayed"),
            rs.getInt("totalPoints"),
            rs.getInt("inspirationCount"),
            rs.getBigDecimal("averagePointsPerGame"),
            instant(rs, "lastGameAt"),
            instant(rs, "lastStatsAt"),
            instant(rs, "updatedAt")
        ), id);
  }

  private MasterAccessRequestRowDto masterAccessRequestById(String id) {
    return jdbcTemplate.queryForObject("""
        select "id", "displayName", "email", "telegramUsername", "requestedRole",
               "status"::text as "status", "createdAt", "updatedAt"
        from "MasterAccessRequest"
        where "id" = ?
        """, (rs, rowNum) -> new MasterAccessRequestRowDto(
            rs.getString("id"),
            rs.getString("displayName"),
            rs.getString("email"),
            rs.getString("telegramUsername"),
            rs.getString("requestedRole"),
            rs.getString("status"),
            instant(rs, "createdAt"),
            instant(rs, "updatedAt")
        ), id);
  }

  private MasterProfileDto profileBySession(HttpServletRequest request) {
    var session = request.getSession(false);
    var profileMode = session == null ? "" : String.valueOf(session.getAttribute("taverna.master.profileMode"));
    var canSwitch = session != null && "admin".equalsIgnoreCase(String.valueOf(session.getAttribute("taverna.master.baseRole")));
    if (canSwitch && "master".equals(profileMode)) {
      var rows = jdbcTemplate.query("""
          select *
          from "Master"
          where "status" = 'active'
            and lower("displayName") like '%александр%'
          order by "updatedAt" desc
          limit 1
          """, (rs, rowNum) -> masterProfile(rs));
      if (!rows.isEmpty()) return rows.get(0);
    }
    var telegram = session == null ? null : normalizeTelegram(String.valueOf(session.getAttribute("taverna.master.telegramUsername")));
    if (telegram != null) {
      var rows = jdbcTemplate.query("""
          select *
          from "Master"
          where lower(replace(coalesce("telegramUsername", ''), '@', '')) = ?
             or lower(replace(coalesce("contactUrl", ''), '@', '')) like ?
          order by "updatedAt" desc
          limit 1
          """, (rs, rowNum) -> masterProfile(rs), telegram, "%" + telegram + "%");
      if (!rows.isEmpty()) return rows.get(0);
    }
    var displayName = session == null ? null : String.valueOf(session.getAttribute("taverna.master.displayName"));
    var rows = jdbcTemplate.query("""
        select *
        from "Master"
        where "status" = 'active'
        order by case when "displayName" = ? then 0 else 1 end, "displayName" asc
        limit 1
        """, (rs, rowNum) -> masterProfile(rs), displayName);
    if (rows.isEmpty()) throw new IllegalArgumentException("Профиль мастера не найден.");
    return rows.get(0);
  }

  private MasterProfileDto masterProfileById(String id) {
    return jdbcTemplate.queryForObject("""
        select *
        from "Master"
        where "id" = ?
        """, (rs, rowNum) -> masterProfile(rs), id);
  }

  private MasterProfileDto masterProfile(ResultSet rs) throws SQLException {
    return new MasterProfileDto(
        rs.getString("id"),
        rs.getString("displayName"),
        rs.getString("telegramUsername"),
        rs.getString("contactUrl"),
        rs.getString("profilePhotoUrl"),
        rs.getString("profileStatus"),
        rs.getString("profileBio"),
        rs.getString("profileStyle"),
        rs.getString("profileInterests"),
        rs.getString("profileSystems"),
        rs.getString("profileExperience"),
        rs.getString("phone"),
        rs.getString("extraLinks"),
        rs.getString("role"),
        rs.getString("status"),
        instant(rs, "updatedAt")
    );
  }

  private MasterOptionDto findMaster(String masterId) {
    var requestedId = masterId == null || masterId.isBlank() ? null : masterId.trim();
    var rows = requestedId == null
        ? jdbcTemplate.query("""
            select "id", "displayName", "contactUrl"
            from "Master"
            where "status" = 'active'
            order by "displayName" asc
            limit 1
            """, (rs, rowNum) -> new MasterOptionDto(rs.getString("id"), rs.getString("displayName"), rs.getString("contactUrl")))
        : jdbcTemplate.query("""
            select "id", "displayName", "contactUrl"
            from "Master"
            where "id" = ?
            limit 1
            """, (rs, rowNum) -> new MasterOptionDto(rs.getString("id"), rs.getString("displayName"), rs.getString("contactUrl")), requestedId);
    if (rows.isEmpty()) throw new IllegalArgumentException("Мастер для игры не найден.");
    return rows.get(0);
  }

  private static String required(String value, String field) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Поле " + field + " обязательно.");
    return value.trim();
  }

  private static String textOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Integer positiveOrDefault(Integer value, int fallback) {
    return value == null || value < 1 ? fallback : value;
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static String blankToSystem(String value) {
    return value == null || value.isBlank() ? "system" : value;
  }

  private static String publicTelegramHandle(String username) {
    return username == null || username.isBlank() ? "" : "@" + username.replaceFirst("^@", "");
  }

  private static String normalizeTelegram(String username) {
    if (username == null || username.isBlank() || "null".equals(username)) return null;
    return username
        .replace("https://t.me/", "")
        .replace("http://t.me/", "")
        .replace("@", "")
        .replace("/", "")
        .trim()
        .toLowerCase();
  }

  public record DashboardResponse(
      String source,
      Instant generatedAt,
      List<MetricDto> metrics,
      List<GameRowDto> upcomingGames,
      List<ActionDto> recentActions
  ) {
  }

  public record ItemsResponse<T>(List<T> content, int page, int size) {
  }

  public record MetricDto(String label, int value, String tone) {
  }

  public record GameRowDto(
      String id,
      String title,
      Instant startsAt,
      String masterPublicId,
      String masterName,
      String gameSystem,
      String status
  ) {
  }

  public record ControlRecordDto(String publicId, String title, String status, Instant updatedAt) {
  }

  public record SignupSummaryDto(
      String gameId,
      String gameTitle,
      Instant startsAt,
      String masterName,
      int confirmedSignups,
      int confirmedSeats,
      int maxPlayers
  ) {
  }

  public record RatingPlayerRowDto(
      int rank,
      String publicId,
      String displayName,
      String nickname,
      boolean visible,
      int gamesPlayed,
      int totalPoints,
      int inspirationCount,
      BigDecimal averagePointsPerGame,
      Instant lastGameAt,
      Instant lastStatsAt,
      Instant updatedAt
  ) {
  }

  public record GalleryPostRowDto(
      String publicId,
      String type,
      String title,
      String category,
      String status,
      boolean visible,
      int mediaCount,
      String previewUrl,
      String authorName,
      Instant eventDate,
      Instant publishedAt,
      Instant createdAt,
      Instant updatedAt
  ) {
    public GalleryPostRowDto(
        String publicId,
        String type,
        String title,
        String category,
        String status,
        boolean visible,
        int mediaCount,
        String authorName,
        Instant eventDate,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
      this(publicId, type, title, category, status, visible, mediaCount, null, authorName, eventDate, publishedAt, createdAt, updatedAt);
    }
  }

  public record GalleryPostDetailsDto(
      String publicId,
      String type,
      String title,
      String description,
      String storyHtml,
      String category,
      String status,
      boolean visible,
      String authorName,
      Instant eventDate,
      Instant publishedAt,
      Instant createdAt,
      Instant updatedAt,
      List<GalleryMediaDto> media
  ) {
    GalleryPostDetailsDto withMedia(List<GalleryMediaDto> media) {
      return new GalleryPostDetailsDto(publicId, type, title, description, storyHtml, category, status, visible,
          authorName, eventDate, publishedAt, createdAt, updatedAt, media);
    }
  }

  public record GalleryMediaDto(
      String id,
      String fileUrl,
      String thumbnailUrl,
      String mediumUrl,
      Integer width,
      Integer height,
      String mimeType,
      String altText
  ) {
  }

  public record RatingAdjustmentRequest(Integer gamesDelta, Integer pointsDelta, Integer inspirationDelta, String reason) {
  }

  public record MasterAccessRequestRowDto(
      String publicId,
      String displayName,
      String email,
      String telegramUsername,
      String requestedRole,
      String status,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  public record MasterProfileDto(
      String id,
      String displayName,
      String telegramUsername,
      String contactUrl,
      String photoUrl,
      String statusText,
      String bio,
      String style,
      String interests,
      String systems,
      String experience,
      String phone,
      String extraLinks,
      String role,
      String accountStatus,
      Instant updatedAt
  ) {
  }

  public record MasterProfileRequest(
      String displayName,
      String telegramUsername,
      String contactUrl,
      String photoUrl,
      String status,
      String bio,
      String style,
      String interests,
      String systems,
      String experience,
      String phone,
      String extraLinks
  ) {
  }

  public record ActionDto(String actorPublicId, String action, String entityType, Instant createdAt) {
  }

  public record ProjectDto(String code, String name, String stack, String detectedPath, String status, String launchMode) {
  }

  public record AccountDto(String publicId, String email, List<String> roles, String status) {
  }

  public record SettingDto(String key, String value, boolean sensitive, boolean encrypted) {
  }

  public record StorageDto(StorageAdapterDto media, StorageAdapterDto projectArtifacts, List<String> futureAdapters) {
  }

  public record StorageAdapterDto(String adapter, String root) {
  }

  public record BackupJobDto(String publicId, String status, String checksum, String manifestPath) {
  }

  public record MasterOptionDto(String id, String displayName, String contactUrl) {
  }

  public record AdminGameRequest(
      String title,
      String description,
      String gameSystem,
      String experienceLevel,
      String ageRating,
      String startsAt,
      Integer durationMinutes,
      Integer minPlayers,
      Integer maxPlayers,
      BigDecimal price,
      String currency,
      String masterPublicId,
      String contactUrl,
      String staffNotes
  ) {
  }
}
