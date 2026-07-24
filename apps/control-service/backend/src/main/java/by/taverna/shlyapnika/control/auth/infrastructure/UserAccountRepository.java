package by.taverna.shlyapnika.control.auth.infrastructure;

import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.auth.domain.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByEmail(String email);
  Optional<UserAccount> findByPublicId(String publicId);
  boolean existsByEmail(String email);
  @Query("select count(u) from UserAccount u join u.roles r where r = :role")
  long countByRole(@Param("role") UserRole role);
}
