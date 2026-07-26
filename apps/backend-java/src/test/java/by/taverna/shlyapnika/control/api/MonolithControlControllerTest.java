package by.taverna.shlyapnika.control.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.taverna.shlyapnika.audit.AuditService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonolithControlController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "taverna.site-base-url=http://localhost:8080",
    "taverna.public-uploads-url=/uploads",
    "taverna.file-storage-dir=uploads",
    "taverna.timezone=Europe/Minsk",
    "taverna.cors-allowed-origins=http://localhost:4177",
    "taverna.internal-api-token=test-internal-token",
    "taverna.auto-publish=true",
    "taverna.serve-frontend=false",
    "taverna.frontend-static-dir=static-site"
})
class MonolithControlControllerTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private JdbcTemplate jdbcTemplate;

  @MockBean
  private AuditService auditService;

  @Test
  void returnsMonolithIntegrationStatus() throws Exception {
    mvc.perform(get("/api/v1/admin/integration/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("monolith"))
        .andExpect(jsonPath("$.mainSiteIntegrationEnabled").value(true))
        .andExpect(jsonPath("$.telegramIntegrationEnabled").value(true));
  }

  @Test
  void returnsEmptyListForUnknownDataSection() throws Exception {
    mvc.perform(get("/api/v1/admin/data/unknown"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void returnsMonolithProjectList() throws Exception {
    mvc.perform(get("/api/v1/admin/projects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("site-monolith"))
        .andExpect(jsonPath("$[1].code").value("telegram-bot"))
        .andExpect(jsonPath("$[2].code").value("master-cabinet"));
  }

  @Test
  void returnsSafeRuntimeSettingsForMasterCabinet() throws Exception {
    mvc.perform(get("/api/v1/admin/settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featureFlags.autoPublishGames").value(true))
        .andExpect(jsonPath("$.storedSettings[0].key").value("timezone"))
        .andExpect(jsonPath("$.storedSettings[0].value").value("Europe/Minsk"))
        .andExpect(jsonPath("$.storedSettings[5].key").value("internalApiToken"))
        .andExpect(jsonPath("$.storedSettings[5].value").value("configured"))
        .andExpect(jsonPath("$.storedSettings[5].sensitive").value(true));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void filtersScheduleByRequestedRange() throws Exception {
    var from = Instant.parse("2026-07-20T00:00:00Z");
    var to = Instant.parse("2026-07-27T00:00:00Z");
    when(jdbcTemplate.query(
        contains("where g.\"dateTimeStart\" >= ?"),
        any(RowMapper.class),
        eq(Timestamp.from(from)),
        eq(Timestamp.from(to)),
        eq(100)
    )).thenReturn(List.of(new MonolithControlController.GameRowDto(
        "gm_1",
        "Игра недели",
        Instant.parse("2026-07-22T18:00:00Z"),
        "mst_1",
        "Александр",
        "D&D 5e",
        "published"
    )));

    mvc.perform(get("/api/v1/admin/schedule")
            .param("from", from.toString())
            .param("to", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value("gm_1"))
        .andExpect(jsonPath("$.content[0].title").value("Игра недели"));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void returnsSignupSummaryWithoutPlayerContacts() throws Exception {
    when(jdbcTemplate.query(contains("left join \"GameSignup\""), any(RowMapper.class), eq(50), eq(0)))
        .thenReturn(List.of(new MonolithControlController.SignupSummaryDto(
            "gm_1",
            "Игра недели",
            Instant.parse("2026-07-22T18:00:00Z"),
            "Александр",
            3,
            4,
            5
        )));

    mvc.perform(get("/api/v1/admin/signups/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].gameId").value("gm_1"))
        .andExpect(jsonPath("$.content[0].confirmedSignups").value(3))
        .andExpect(jsonPath("$.content[0].confirmedSeats").value(4))
        .andExpect(jsonPath("$.content[0].maxPlayers").value(5))
        .andExpect(jsonPath("$.content[0].contact").doesNotExist())
        .andExpect(jsonPath("$.content[0].playerName").doesNotExist());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void returnsRatingPlayersForMasterCabinet() throws Exception {
    when(jdbcTemplate.query(contains("from \"RatingPlayer\""), any(RowMapper.class), eq(50), eq(0)))
        .thenReturn(List.of(new MonolithControlController.RatingPlayerRowDto(
            1,
            "rtp_1",
            "Артём",
            "Покер Шлявин",
            true,
            3,
            42,
            2,
            new BigDecimal("14.00"),
            Instant.parse("2026-07-20T18:00:00Z"),
            Instant.parse("2026-07-20T21:00:00Z"),
            Instant.parse("2026-07-20T21:00:00Z")
        )));

    mvc.perform(get("/api/v1/admin/rating/players"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].rank").value(1))
        .andExpect(jsonPath("$.content[0].displayName").value("Артём"))
        .andExpect(jsonPath("$.content[0].nickname").value("Покер Шлявин"))
        .andExpect(jsonPath("$.content[0].totalPoints").value(42))
        .andExpect(jsonPath("$.content[0].inspirationCount").value(2));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void returnsGalleryPostsForMasterCabinet() throws Exception {
    when(jdbcTemplate.query(contains("from \"GalleryPost\""), any(RowMapper.class), eq(null), eq(null), eq(50), eq(0)))
        .thenReturn(List.of(new MonolithControlController.GalleryPostRowDto(
            "gallery-test",
            "story",
            "Прощание Либе",
            "games",
            "published",
            true,
            2,
            "Александр",
            Instant.parse("2026-07-18T18:00:00Z"),
            Instant.parse("2026-07-19T12:00:00Z"),
            Instant.parse("2026-07-19T12:00:00Z"),
            Instant.parse("2026-07-19T12:00:00Z")
        )));

    mvc.perform(get("/api/v1/admin/gallery/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].publicId").value("gallery-test"))
        .andExpect(jsonPath("$.content[0].type").value("story"))
        .andExpect(jsonPath("$.content[0].title").value("Прощание Либе"))
        .andExpect(jsonPath("$.content[0].mediaCount").value(2))
        .andExpect(jsonPath("$.content[0].authorName").value("Александр"));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void publishesGalleryPostFromMasterCabinet() throws Exception {
    when(jdbcTemplate.update(contains("update \"GalleryPost\""), eq("published"), eq(true), eq("published"), eq("gallery-test")))
        .thenReturn(1);
    when(jdbcTemplate.queryForObject(contains("where p.\"publicId\" = ?"), any(RowMapper.class), eq("gallery-test")))
        .thenReturn(new MonolithControlController.GalleryPostRowDto(
            "gallery-test",
            "photo",
            "Вечер за столом",
            "games",
            "published",
            true,
            1,
            "Александр",
            null,
            Instant.parse("2026-07-19T12:00:00Z"),
            Instant.parse("2026-07-19T12:00:00Z"),
            Instant.parse("2026-07-19T12:00:00Z")
        ));

    mvc.perform(post("/api/v1/admin/gallery/posts/gallery-test/publish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("gallery-test"))
        .andExpect(jsonPath("$.status").value("published"))
        .andExpect(jsonPath("$.visible").value(true));

    verify(auditService).write(
        eq("master-cabinet"),
        eq("gallery.post_published_from_cabinet"),
        eq("GalleryPost"),
        eq("gallery-test"),
        contains("\"status\":\"published\"")
    );
  }

  @Test
  void deletesGalleryPostFromMasterCabinet() throws Exception {
    when(jdbcTemplate.update(contains("delete from \"GalleryPost\""), eq("gallery-test")))
        .thenReturn(1);

    mvc.perform(delete("/api/v1/admin/gallery/posts/gallery-test"))
        .andExpect(status().isNoContent());

    verify(auditService).write(
        eq("master-cabinet"),
        eq("gallery.post_deleted_from_cabinet"),
        eq("GalleryPost"),
        eq("gallery-test"),
        eq(null)
    );
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void marksServiceRequestAsContactedFromMasterCabinet() throws Exception {
    when(jdbcTemplate.update(contains("update \"ServiceRequest\""), eq("contacted"), eq("request-test")))
        .thenReturn(1);
    when(jdbcTemplate.queryForObject(contains("from \"ServiceRequest\""), any(RowMapper.class), eq("request-test")))
        .thenReturn(new MonolithControlController.ControlRecordDto(
            "request-test",
            "Заказная игра",
            "contacted",
            Instant.parse("2026-07-19T12:00:00Z")
        ));

    mvc.perform(post("/api/v1/admin/service-requests/request-test/contact"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("request-test"))
        .andExpect(jsonPath("$.status").value("contacted"));

    verify(auditService).write(
        eq("master-cabinet"),
        eq("service_request.contacted_from_cabinet"),
        eq("ServiceRequest"),
        eq("request-test"),
        contains("\"status\":\"contacted\"")
    );
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void blocksMasterFromMasterCabinet() throws Exception {
    when(jdbcTemplate.update(contains("update \"Master\""), eq("blocked"), eq("mst_test")))
        .thenReturn(1);
    when(jdbcTemplate.queryForObject(contains("from \"Master\""), any(RowMapper.class), eq("mst_test")))
        .thenReturn(new MonolithControlController.ControlRecordDto(
            "mst_test",
            "Александр",
            "blocked",
            Instant.parse("2026-07-19T12:00:00Z")
        ));

    mvc.perform(post("/api/v1/admin/masters/mst_test/block"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value("mst_test"))
        .andExpect(jsonPath("$.status").value("blocked"));

    verify(auditService).write(
        eq("master-cabinet"),
        eq("master.blocked_from_cabinet"),
        eq("Master"),
        eq("mst_test"),
        contains("\"status\":\"blocked\"")
    );
  }

}
