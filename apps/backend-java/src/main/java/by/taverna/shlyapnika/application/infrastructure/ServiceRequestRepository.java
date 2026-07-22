package by.taverna.shlyapnika.application.infrastructure;

import by.taverna.shlyapnika.application.domain.ServiceRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequestEntity, String> {
}
