package by.taverna.shlyapnika.control.roles.api;

import by.taverna.shlyapnika.control.auth.domain.UserRole;
import by.taverna.shlyapnika.control.security.RoleCatalog;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class RolesController {
  @GetMapping
  @PreAuthorize("hasAuthority('users.read')")
  Map<UserRole, Set<String>> roles() {
    return RoleCatalog.all().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(permission -> permission.value()).collect(Collectors.toSet())));
  }
}
