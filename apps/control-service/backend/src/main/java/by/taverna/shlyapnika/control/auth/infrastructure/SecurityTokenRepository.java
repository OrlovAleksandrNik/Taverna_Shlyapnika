package by.taverna.shlyapnika.control.auth.infrastructure;

import by.taverna.shlyapnika.control.auth.domain.SecurityToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityTokenRepository extends JpaRepository<SecurityToken, UUID> {
  Optional<SecurityToken> findByTokenHash(String tokenHash);
}
