package by.taverna.shlyapnika.control.common;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataRecordRepository extends JpaRepository<DataRecord, UUID> {
  Optional<DataRecord> findBySectionAndPublicIdAndDeletedAtIsNull(String section, String publicId);

  @Query("""
      select record from DataRecord record
      where record.section = :section
        and record.deletedAt is null
        and (:query = '' or lower(record.title) like lower(concat('%', :query, '%')) or lower(record.payload) like lower(concat('%', :query, '%')))
      """)
  Page<DataRecord> search(@Param("section") String section, @Param("query") String query, Pageable pageable);
}
