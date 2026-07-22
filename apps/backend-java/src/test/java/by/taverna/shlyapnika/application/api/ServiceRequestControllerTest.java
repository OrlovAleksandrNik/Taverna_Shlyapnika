package by.taverna.shlyapnika.application.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.taverna.shlyapnika.application.ServiceRequestService;
import by.taverna.shlyapnika.application.domain.ServiceRequestEntity;
import by.taverna.shlyapnika.common.ConsentRequiredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ServiceRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceRequestControllerTest {
  @Autowired
  private MockMvc mvc;

  @MockBean
  private ServiceRequestService service;

  @Test
  void createsServiceRequestWithConsent() throws Exception {
    var saved = mock(ServiceRequestEntity.class);
    when(saved.getId()).thenReturn("srv_1");
    when(service.create(any())).thenReturn(saved);

    mvc.perform(post("/api/service-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Гость",
                  "contact": "@guest",
                  "service": "Заказная игра",
                  "serviceType": "custom-game",
                  "participants": 5,
                  "consentGiven": true,
                  "consentVersion": "1.0",
                  "privacyPolicyVersion": "1.0"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.requestId").value("srv_1"));

    verify(service).create(any(ServiceRequestRequest.class));
  }

  @Test
  void returnsConsentErrorWhenServiceRejectsRequest() throws Exception {
    when(service.create(any())).thenThrow(new ConsentRequiredException());

    mvc.perform(post("/api/service-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Гость",
                  "contact": "@guest",
                  "service": "Заказная игра",
                  "consentGiven": false
                }
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
  }
}
