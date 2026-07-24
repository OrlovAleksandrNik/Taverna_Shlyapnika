package by.taverna.shlyapnika.control.masters.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/masters")
@PreAuthorize("hasAuthority('masters.manage')")
public class MastersController extends SectionStubController {
  public MastersController() { super("masters"); }
}
