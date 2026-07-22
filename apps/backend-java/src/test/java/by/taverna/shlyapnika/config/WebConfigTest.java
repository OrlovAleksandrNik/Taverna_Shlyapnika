package by.taverna.shlyapnika.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.schedule.ScheduleService;
import by.taverna.shlyapnika.schedule.api.ScheduleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
@TestPropertySource(properties = {
    "taverna.site-base-url=https://taverna.example",
    "taverna.public-uploads-url=/uploads",
    "taverna.file-storage-dir=uploads",
    "taverna.timezone=Europe/Minsk",
    "taverna.cors-allowed-origins=https://taverna.example,http://localhost:4177",
    "taverna.internal-api-token=test-internal-token",
    "taverna.serve-frontend=false",
    "taverna.frontend-static-dir=static-site"
})
class WebConfigTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private ScheduleService schedule;

  @Test
  void allowsConfiguredCorsOriginForPublicApi() throws Exception {
    mvc.perform(options("/api/games")
            .header(HttpHeaders.ORIGIN, "https://taverna.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://taverna.example"));
  }

  @Test
  void allowsNullOriginForLocalFilePreview() throws Exception {
    mvc.perform(options("/api/games")
            .header(HttpHeaders.ORIGIN, "null")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "null"));
  }
}
