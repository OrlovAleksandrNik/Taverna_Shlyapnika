package by.taverna.shlyapnika.access.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.access.MasterAccessService;
import by.taverna.shlyapnika.common.ConsentRequiredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
                  "telegramUsername": "@MisterHatter"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessGranted").value(true))
        .andExpect(jsonPath("$.displayName").value("Александр"))
        .andExpect(jsonPath("$.role").value("admin"));
  }
}
