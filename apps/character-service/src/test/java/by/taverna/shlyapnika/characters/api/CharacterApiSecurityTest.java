package by.taverna.shlyapnika.characters.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "character-service.api-token=secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CharacterApiSecurityTest {
  @Autowired
  private MockMvc mockMvc;

  @Test
  void healthIsPublic() throws Exception {
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk());
  }

  @Test
  void apiRequiresTokenWhenConfigured() throws Exception {
    mockMvc.perform(get("/api/v1/sheet-templates/dnd5e"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiAcceptsBearerToken() throws Exception {
    mockMvc.perform(get("/api/v1/sheet-templates/dnd5e").header("Authorization", "Bearer secret"))
        .andExpect(status().isOk());
  }
}
