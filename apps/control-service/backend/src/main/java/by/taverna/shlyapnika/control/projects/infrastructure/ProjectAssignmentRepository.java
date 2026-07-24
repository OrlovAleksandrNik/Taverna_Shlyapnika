package by.taverna.shlyapnika.control.projects.infrastructure;

import by.taverna.shlyapnika.control.projects.domain.ProjectAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, UUID> {
  List<ProjectAssignment> findByProjectCodeOrderByCreatedAtDesc(String projectCode);
}
