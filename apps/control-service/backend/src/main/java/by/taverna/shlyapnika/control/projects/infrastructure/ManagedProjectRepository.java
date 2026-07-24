package by.taverna.shlyapnika.control.projects.infrastructure;

import by.taverna.shlyapnika.control.projects.domain.ManagedProject;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagedProjectRepository extends JpaRepository<ManagedProject, UUID> {
  Optional<ManagedProject> findByCode(String code);
  boolean existsByCode(String code);
}
