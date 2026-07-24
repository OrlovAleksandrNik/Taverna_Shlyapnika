package by.taverna.shlyapnika.control.permissions.api;

import by.taverna.shlyapnika.control.security.Permission;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/permissions")
public class PermissionsController {
  @GetMapping
  @PreAuthorize("hasAuthority('users.read')")
  List<String> permissions() {
    return Arrays.stream(Permission.values()).map(Permission::value).sorted().toList();
  }
}
