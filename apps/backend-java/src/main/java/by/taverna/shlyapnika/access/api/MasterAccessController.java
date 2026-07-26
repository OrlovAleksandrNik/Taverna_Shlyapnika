package by.taverna.shlyapnika.access.api;

import by.taverna.shlyapnika.access.MasterAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MasterAccessController {
  private final MasterAccessService service;

  public MasterAccessController(MasterAccessService service) {
    this.service = service;
  }

  @PostMapping("/api/auth/master-access-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public MasterAccessResponse requestMasterAccess(@Valid @RequestBody MasterAccessRequest request) {
    return service.requestMasterAccess(request);
  }

  @PostMapping("/api/auth/master-login")
  public MasterAccessResponse login(@Valid @RequestBody MasterLoginRequest request) {
    return service.login(request);
  }
}
