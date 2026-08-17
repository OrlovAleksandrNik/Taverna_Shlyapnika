package by.taverna.shlyapnika.account.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.account.SiteAccountService;
import by.taverna.shlyapnika.account.SiteAccountService.AuthenticatedAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SiteAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class SiteAuthControllerTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private SiteAccountService service;

  @Test
  void registersPlayerAndWritesSession() throws Exception {
    when(service.register(any())).thenReturn(new AuthenticatedAccount(
        "acc_player",
        "Артём",
        "player@example.com",
        "player",
        "active",
        "@player"
    ));

    mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "displayName": "Артём",
                  "email": "player@example.com",
                  "password": "password-1",
                  "passwordConfirmation": "password-1",
                  "accountType": "player",
                  "telegramUsername": "@player",
                  "consentGiven": true,
                  "consentVersion": "1.0",
                  "privacyPolicyVersion": "1.0"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessGranted").value(true))
        .andExpect(jsonPath("$.systemRole").value("PLAYER"))
        .andExpect(jsonPath("$.accountType").value("player"))
        .andExpect(request().sessionAttribute("taverna.master.accessGranted", true))
        .andExpect(request().sessionAttribute("taverna.auth.systemRole", "PLAYER"));
  }

  @Test
  void registersMasterAsPendingWithoutSessionAccess() throws Exception {
    when(service.register(any())).thenReturn(new AuthenticatedAccount(
        "acc_master",
        "Новый мастер",
        "master@example.com",
        "master",
        "pending_approval",
        "@new_master"
    ));

    mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "displayName": "Новый мастер",
                  "email": "master@example.com",
                  "password": "password-1",
                  "passwordConfirmation": "password-1",
                  "accountType": "master",
                  "telegramUsername": "@new_master",
                  "consentGiven": true,
                  "consentVersion": "1.0",
                  "privacyPolicyVersion": "1.0"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessGranted").value(false))
        .andExpect(jsonPath("$.status").value("pending_approval"))
        .andExpect(jsonPath("$.systemRole").value("MASTER"))
        .andExpect(request().sessionAttributeDoesNotExist("taverna.master.accessGranted"));
  }

  @Test
  void logsInActiveMasterAndWritesSession() throws Exception {
    when(service.login(any())).thenReturn(new AuthenticatedAccount(
        "acc_master",
        "Мастер",
        "master@example.com",
        "master",
        "active",
        "@master"
    ));

    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "master@example.com",
                  "password": "password-1"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessGranted").value(true))
        .andExpect(jsonPath("$.systemRole").value("MASTER"))
        .andExpect(request().sessionAttribute("taverna.master.accessGranted", true))
        .andExpect(request().sessionAttribute("taverna.auth.accountId", "acc_master"));
  }
}
