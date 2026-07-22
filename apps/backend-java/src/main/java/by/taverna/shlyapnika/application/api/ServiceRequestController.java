package by.taverna.shlyapnika.application.api;

import by.taverna.shlyapnika.application.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceRequestController {
  private final ServiceRequestService service;

  public ServiceRequestController(ServiceRequestService service) {
    this.service = service;
  }

  @PostMapping("/api/service-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public ServiceRequestResponse create(@Valid @RequestBody ServiceRequestRequest request) {
    var saved = service.create(request);
    return new ServiceRequestResponse(true, "Заявка сохранена. Мы свяжемся с вами.", saved.getId());
  }
}
