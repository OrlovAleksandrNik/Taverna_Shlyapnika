package by.taverna.shlyapnika.control.players.api;

import by.taverna.shlyapnika.control.common.SectionStubController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/players")
@PreAuthorize("hasAuthority('rating.read')")
public class PlayersController extends SectionStubController {
  public PlayersController() { super("players"); }
}
