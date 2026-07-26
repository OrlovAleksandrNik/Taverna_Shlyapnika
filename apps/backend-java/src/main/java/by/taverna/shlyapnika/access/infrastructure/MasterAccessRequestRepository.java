package by.taverna.shlyapnika.access.infrastructure;

import by.taverna.shlyapnika.access.domain.MasterAccessRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterAccessRequestRepository extends JpaRepository<MasterAccessRequestEntity, String> {
  List<MasterAccessRequestEntity> findByStatusOrderByCreatedAtAsc(String status);

  Optional<MasterAccessRequestEntity> findFirstByNormalizedTelegramUsernameAndStatusOrderByCreatedAtDesc(String normalizedTelegramUsername, String status);

  Optional<MasterAccessRequestEntity> findFirstByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String email, String status);
}
