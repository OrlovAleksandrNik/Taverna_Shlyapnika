package by.taverna.shlyapnika.master.infrastructure;

import by.taverna.shlyapnika.master.domain.MasterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MasterRepository extends JpaRepository<MasterEntity, String> {
  Optional<MasterEntity> findByTelegramUserId(Long telegramUserId);

  @Query(value = """
      select * from "Master"
      where "status" = cast('active' as "MasterStatus")
      order by "displayName" asc
      """, nativeQuery = true)
  List<MasterEntity> findActiveMasters();

  @Query(value = """
      select count(*) from "Master"
      where "role" = cast('admin' as "MasterRole")
      """, nativeQuery = true)
  long countAdmins();
}
