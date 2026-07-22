package by.taverna.shlyapnika.schedule.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.common.ConsentRequiredException;
import by.taverna.shlyapnika.schedule.ScheduleService;
import by.taverna.shlyapnika.schedule.ScheduleService.GameSignupResult;
import by.taverna.shlyapnika.schedule.api.GameResponses.MasterDto;
import by.taverna.shlyapnika.schedule.api.GameResponses.PublicGameDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScheduleControllerTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private ScheduleService schedule;

  @Test
  void returnsPublicGamesFromService() throws Exception {
    when(schedule.listPublicGames(any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of(publicGame()));

    mvc.perform(get("/api/games")
            .param("system", "D&D")
            .param("limit", "6"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.games[0].id").value("gm_public"))
        .andExpect(jsonPath("$.games[0].title").value("Проклятие старой башни"))
        .andExpect(jsonPath("$.games[0].master.name").value("Александр"))
        .andExpect(jsonPath("$.games[0].availableSeats").value(3));
  }

  @Test
  void createsGameSignupWithConsent() throws Exception {
    when(schedule.createSignup(any()))
        .thenReturn(new GameSignupResult("sgn_1", publicGame()));

    mvc.perform(post("/api/game-signups")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "gameId": "gm_public",
                  "playerName": "Игрок",
                  "contact": "@player",
                  "seats": 2,
                  "consentGiven": true,
                  "consentVersion": "1.0",
                  "privacyPolicyVersion": "1.0"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.signupId").value("sgn_1"))
        .andExpect(jsonPath("$.game.id").value("gm_public"));

    verify(schedule).createSignup(any(GameSignupRequest.class));
  }

  @Test
  void returnsConsentErrorWhenServiceRejectsSignup() throws Exception {
    when(schedule.createSignup(any())).thenThrow(new ConsentRequiredException());

    mvc.perform(post("/api/game-signups")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "gameId": "gm_public",
                  "playerName": "Игрок",
                  "contact": "@player",
                  "seats": 1,
                  "consentGiven": false
                }
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
  }

  private static PublicGameDto publicGame() {
    var master = new MasterDto("mst_1", "Александр", "https://t.me/master", "master");
    return new PublicGameDto(
        "gm_public",
        "Проклятие старой башни",
        "Короткое приключение для тех, кто любит загадки.",
        "D&D 5e",
        "D&D 5e",
        "D&D 5e",
        "для новичков",
        "12+",
        Instant.parse("2026-08-01T15:00:00Z"),
        "1 августа, суббота 18:00",
        180,
        Instant.parse("2026-08-01T18:00:00Z"),
        3,
        5,
        new BigDecimal("35"),
        "BYN",
        null,
        "https://t.me/master",
        "published",
        "mst_1",
        "Александр",
        "master",
        master,
        2,
        3,
        List.of("dnd", "newbie")
    );
  }
}
