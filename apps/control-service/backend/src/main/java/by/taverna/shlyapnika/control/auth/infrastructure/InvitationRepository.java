package by.taverna.shlyapnika.control.auth.infrastructure;

import by.taverna.shlyapnika.control.auth.domain.Invitation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
  Optional<Invitation> findByTokenHash(String tokenHash);
}
