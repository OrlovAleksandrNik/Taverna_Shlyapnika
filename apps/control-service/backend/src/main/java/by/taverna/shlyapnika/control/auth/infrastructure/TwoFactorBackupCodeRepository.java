package by.taverna.shlyapnika.control.auth.infrastructure;

import by.taverna.shlyapnika.control.auth.domain.TwoFactorBackupCode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorBackupCodeRepository extends JpaRepository<TwoFactorBackupCode, UUID> {
  List<TwoFactorBackupCode> findByUserIdAndUsedAtIsNull(UUID userId);
  void deleteByUserId(UUID userId);
}
