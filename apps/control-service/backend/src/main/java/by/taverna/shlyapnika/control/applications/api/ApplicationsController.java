package by.taverna.shlyapnika.control.applications.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/applications")
@PreAuthorize("hasAuthority('applications.manage')")
public class ApplicationsController extends SectionStubController {
  public ApplicationsController() { super("applications"); }
}
