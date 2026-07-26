package by.taverna.shlyapnika.control.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import by.taverna.shlyapnika.audit.AuditService;
import java.math.BigDecimal;
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

}
