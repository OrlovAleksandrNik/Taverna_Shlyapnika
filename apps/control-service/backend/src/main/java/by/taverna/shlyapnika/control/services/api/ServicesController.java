package by.taverna.shlyapnika.control.services.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/services")
@PreAuthorize("hasAuthority('services.manage')")
public class ServicesController extends SectionStubController {
  public ServicesController() { super("services"); }
}
