package by.taverna.shlyapnika.control.files.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/files")
@PreAuthorize("hasAuthority('files.manage')")
public class FilesController extends SectionStubController {
  public FilesController() { super("files"); }
}
