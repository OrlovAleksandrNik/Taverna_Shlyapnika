package by.taverna.shlyapnika.account.infrastructure;

import by.taverna.shlyapnika.account.domain.PasswordResetTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {
  Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
}
