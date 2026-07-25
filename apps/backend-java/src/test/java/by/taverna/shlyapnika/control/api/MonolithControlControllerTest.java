package by.taverna.shlyapnika.control.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonolithControlController.class)
@AutoConfigureMockMvc(addFilters = false)
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
}
