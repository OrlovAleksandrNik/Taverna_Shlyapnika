package by.taverna.shlyapnika.control.notifications.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasAuthority('notifications.manage')")
public class NotificationsController extends SectionStubController {
  public NotificationsController() { super("notifications"); }
}
