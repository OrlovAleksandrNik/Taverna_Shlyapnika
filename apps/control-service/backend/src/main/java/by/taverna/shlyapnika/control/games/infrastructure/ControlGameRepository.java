package by.taverna.shlyapnika.control.games.infrastructure;

import by.taverna.shlyapnika.control.games.domain.ControlGame;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlGameRepository extends JpaRepository<ControlGame, UUID> {
  List<ControlGame> findTop20ByDeletedAtIsNullOrderByStartsAtAsc();
  List<ControlGame> findByDeletedAtIsNullAndStartsAtBetweenOrderByStartsAtAsc(Instant from, Instant to);
}
