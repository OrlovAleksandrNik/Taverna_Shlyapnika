package by.taverna.shlyapnika.access.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.access.MasterAccessService;
import by.taverna.shlyapnika.common.ConsentRequiredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MasterAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
class MasterAccessControllerTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private MasterAccessService service;

  @Test
  void createsMasterAccessRequest() throws Exception {
    when(service.requestMasterAccess(any())).thenReturn(MasterAccessResponse.requested(
        "mac_1",
        "pending",
        "Заявка отправлена Шляпнику на подтверждение."
    ));

    mvc.perform(post("/api/auth/master-access-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "displayName": "Александр",
                  "email": "master@example.com",
                  "telegramUsername": "@MisterHatter",
                  "consentGiven": true,
                  "consentVersion": "1.0",
                  "privacyPolicyVersion": "1.0"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.requestId").value("mac_1"))
        .andExpect(jsonPath("$.status").value("pending"));

    verify(service).requestMasterAccess(any(MasterAccessRequest.class));
  }

  @Test
  void returnsConsentErrorWhenMasterAccessRequestHasNoConsent() throws Exception {
    when(service.requestMasterAccess(any())).thenThrow(new ConsentRequiredException());

    mvc.perform(post("/api/auth/master-access-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "displayName": "Александр",
                  "email": "master@example.com",
                  "telegramUsername": "@MisterHatter",
                  "consentGiven": false
                }
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
  }

  @Test
  void logsInApprovedMaster() throws Exception {
    when(service.login(any())).thenReturn(MasterAccessResponse.login(true, "Добро пожаловать. Дневник открыт.", "Александр", "admin"));

    mvc.perform(post("/api/auth/master-login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "telegramUsername": "@MisterHatter",
                  "email": "master@example.com"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessGranted").value(true))
        .andExpect(jsonPath("$.displayName").value("Александр"))
        .andExpect(jsonPath("$.role").value("admin"))
        .andExpect(request().sessionAttribute("taverna.master.accessGranted", true))
        .andExpect(request().sessionAttribute("taverna.master.displayName", "Александр"))
        .andExpect(request().sessionAttribute("taverna.master.role", "admin"))
        .andExpect(request().sessionAttribute("taverna.master.telegramUsername", "@MisterHatter"))
        .andExpect(request().sessionAttribute("taverna.master.email", "master@example.com"));
  }

  @Test
  void returnsAnonymousSessionWithoutLogin() throws Exception {
    mvc.perform(get("/api/auth/session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessGranted").value(false))
        .andExpect(jsonPath("$.role").value("master"));
  }

  @Test
  void returnsActiveMasterSession() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", "Александр");
    session.setAttribute("taverna.master.role", "admin");
    session.setAttribute("taverna.master.telegramUsername", "@MisterHatter");
    session.setAttribute("taverna.master.email", "master@example.com");

    mvc.perform(get("/api/auth/session").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessGranted").value(true))
        .andExpect(jsonPath("$.displayName").value("Александр"))
        .andExpect(jsonPath("$.role").value("admin"))
        .andExpect(jsonPath("$.telegramUsername").value("@MisterHatter"))
        .andExpect(jsonPath("$.email").value("master@example.com"));
  }

  @Test
  void switchesHatterSessionToMasterMode() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", "Шляпник");
    session.setAttribute("taverna.master.role", "admin");
    session.setAttribute("taverna.master.baseRole", "admin");
    session.setAttribute("taverna.master.profileMode", "hatter");
    session.setAttribute("taverna.master.telegramUsername", "@MisterHatter");
    session.setAttribute("taverna.master.email", "master@example.com");

    mvc.perform(put("/api/auth/session-mode").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"master\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Мастер Александр"))
        .andExpect(jsonPath("$.role").value("master"))
        .andExpect(jsonPath("$.baseRole").value("admin"))
        .andExpect(jsonPath("$.profileMode").value("master"))
        .andExpect(jsonPath("$.canSwitchProfile").value(true));
  }

  @Test
  void keepsInferredBaseRoleWhenOldHatterSessionSwitchesTwice() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", "РЁР»СЏРїРЅРёРє");
    session.setAttribute("taverna.master.role", "admin");
    session.setAttribute("taverna.master.profileMode", "hatter");

    mvc.perform(put("/api/auth/session-mode").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"master\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.baseRole").value("admin"))
        .andExpect(jsonPath("$.canSwitchProfile").value(true));

    mvc.perform(put("/api/auth/session-mode").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"hatter\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("admin"))
        .andExpect(jsonPath("$.profileMode").value("hatter"));
  }

  @Test
  void regularMasterCannotSwitchSessionMode() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("taverna.master.accessGranted", true);
    session.setAttribute("taverna.master.displayName", "Мастер");
    session.setAttribute("taverna.master.role", "master");
    session.setAttribute("taverna.master.baseRole", "master");
    session.setAttribute("taverna.master.profileMode", "master");

    mvc.perform(put("/api/auth/session-mode").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"hatter\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void logsOutMasterSession() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute("taverna.master.accessGranted", true);

    mvc.perform(post("/api/auth/logout").session(session))
        .andExpect(status().isNoContent());
  }
}
