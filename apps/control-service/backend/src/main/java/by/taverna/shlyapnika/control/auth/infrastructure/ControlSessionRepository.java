package by.taverna.shlyapnika.control.auth.infrastructure;

import by.taverna.shlyapnika.control.auth.domain.ControlSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlSessionRepository extends JpaRepository<ControlSession, UUID> {
  List<ControlSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
  Optional<ControlSession> findBySessionHash(String sessionHash);
}
