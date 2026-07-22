package by.taverna.shlyapnika.schedule.infrastructure;

import by.taverna.shlyapnika.schedule.domain.GameSignupEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameSignupRepository extends JpaRepository<GameSignupEntity, String> {
  @Query(value = """
      select coalesce(sum("seats"), 0)
      from "GameSignup"
      where "gameId" = :gameId
        and "status" = cast('confirmed' as "SignupStatus")
      """, nativeQuery = true)
  int confirmedSeats(@Param("gameId") String gameId);

  Optional<GameSignupEntity> findByGame_IdAndContact(String gameId, String contact);
}
