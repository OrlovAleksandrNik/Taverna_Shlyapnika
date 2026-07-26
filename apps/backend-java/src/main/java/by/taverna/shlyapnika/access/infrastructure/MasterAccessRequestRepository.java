package by.taverna.shlyapnika.access.infrastructure;

import by.taverna.shlyapnika.access.domain.MasterAccessRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MasterAccessRequestRepository extends JpaRepository<MasterAccessRequestEntity, String> {
  @Query(value = """
      select *
      from "MasterAccessRequest"
      where "status" = cast(:status as "MasterAccessRequestStatus")
      order by "createdAt" asc
      """, nativeQuery = true)
  List<MasterAccessRequestEntity> findByStatusOrderByCreatedAtAsc(@Param("status") String status);

  @Query(value = """
      select *
      from "MasterAccessRequest"
      where "normalizedTelegramUsername" = :normalizedTelegramUsername
        and "status" = cast(:status as "MasterAccessRequestStatus")
      order by "createdAt" desc
      limit 1
      """, nativeQuery = true)
  Optional<MasterAccessRequestEntity> findFirstByNormalizedTelegramUsernameAndStatusOrderByCreatedAtDesc(
      @Param("normalizedTelegramUsername") String normalizedTelegramUsername,
      @Param("status") String status
  );

  @Query(value = """
      select *
      from "MasterAccessRequest"
      where lower("email") = lower(:email)
        and "status" = cast(:status as "MasterAccessRequestStatus")
      order by "createdAt" desc
      limit 1
      """, nativeQuery = true)
  Optional<MasterAccessRequestEntity> findFirstByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
      @Param("email") String email,
      @Param("status") String status
  );
}
