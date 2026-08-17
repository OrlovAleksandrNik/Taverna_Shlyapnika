package by.taverna.shlyapnika.account.infrastructure;

import by.taverna.shlyapnika.account.domain.SiteAccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteAccountRepository extends JpaRepository<SiteAccountEntity, String> {
  @Query(value = "select * from \"SiteAccount\" where lower(\"email\") = lower(:email) limit 1", nativeQuery = true)
  Optional<SiteAccountEntity> findByEmailIgnoreCase(@Param("email") String email);

  Optional<SiteAccountEntity> findFirstByNormalizedTelegramUsernameOrderByCreatedAtDesc(String normalizedTelegramUsername);

  List<SiteAccountEntity> findByRoleAndStatusOrderByCreatedAtAsc(String role, String status);
}
