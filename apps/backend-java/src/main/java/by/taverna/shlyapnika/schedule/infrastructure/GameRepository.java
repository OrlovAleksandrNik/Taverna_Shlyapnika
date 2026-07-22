package by.taverna.shlyapnika.schedule.infrastructure;

import by.taverna.shlyapnika.schedule.domain.GameEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<GameEntity, String> {
  @EntityGraph(attributePaths = "master")
  @Query(value = """
      select * from "Game"
      where "status" = cast(:status as "GameStatus")
        and "dateTimeStart" >= :from
      order by "dateTimeStart" asc
      limit 100
      """, nativeQuery = true)
  List<GameEntity> findPublicGames(@Param("status") String status, @Param("from") Instant from);

  @EntityGraph(attributePaths = "master")
  @Query(value = """
      select * from "Game"
      where "id" = :id
        and "status" = cast(:status as "GameStatus")
        and "dateTimeStart" >= :from
      limit 1
      """, nativeQuery = true)
  Optional<GameEntity> findPublicGame(@Param("id") String id, @Param("status") String status, @Param("from") Instant from);

  @EntityGraph(attributePaths = "master")
  @Query(value = """
      select * from "Game"
      where "masterId" = :masterId
      order by "dateTimeStart" asc
      limit 100
      """, nativeQuery = true)
  List<GameEntity> findByMasterIdForBot(@Param("masterId") String masterId);

  @EntityGraph(attributePaths = "master")
  @Query(value = """
      select * from "Game"
      where "id" = :id
        and "masterId" = :masterId
      limit 1
      """, nativeQuery = true)
  Optional<GameEntity> findByIdAndMasterIdForBot(@Param("id") String id, @Param("masterId") String masterId);
}
