package by.taverna.shlyapnika.control.common;

import by.taverna.shlyapnika.control.audit.application.AuditService;
import by.taverna.shlyapnika.control.common.DataRecordDtos.DataRecordRequest;
import by.taverna.shlyapnika.control.common.DataRecordDtos.DataRecordResponse;
import jakarta.transaction.Transactional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DataRecordService {
  private final DataRecordRepository records;
  private final AuditService audit;

  public DataRecordService(DataRecordRepository records, AuditService audit) {
    this.records = records;
    this.audit = audit;
  }

  public Page<DataRecordResponse> list(String section, String query, int page, int size) {
    int safeSize = Math.max(1, Math.min(size, 100));
    return records.search(section, query == null ? "" : query.trim(), PageRequest.of(Math.max(0, page), safeSize, Sort.by(Sort.Direction.DESC, "updatedAt")))
        .map(DataRecordService::toResponse);
  }

  @Transactional
  public DataRecordResponse create(String section, DataRecordRequest request, String actor, String ipAddress) {
    DataRecord saved = records.save(new DataRecord(section, request.title(), request.payload(), actor));
    audit.record(actor, "data.create", "DataRecord", saved.getPublicId(), "section=" + section, ipAddress);
    return toResponse(saved);
  }

  @Transactional
  public DataRecordResponse update(String section, String publicId, DataRecordRequest request, String actor, String ipAddress) {
    DataRecord record = find(section, publicId);
    if (request.version() != null && !request.version().equals(record.getVersion())) {
      throw new IllegalArgumentException("Record was changed. Reload before saving.");
    }
    record.update(request.title(), request.payload());
    audit.record(actor, "data.update", "DataRecord", publicId, "section=" + section, ipAddress);
    return toResponse(records.save(record));
  }

  @Transactional
  public DataRecordResponse publish(String section, String publicId, String actor, String ipAddress) {
    DataRecord record = find(section, publicId);
    record.publish();
    audit.record(actor, "data.publish", "DataRecord", publicId, "section=" + section, ipAddress);
    return toResponse(records.save(record));
  }

  @Transactional
  public void softDelete(String section, String publicId, String actor, String ipAddress) {
    DataRecord record = find(section, publicId);
    record.softDelete();
    records.save(record);
    audit.record(actor, "data.soft_delete", "DataRecord", publicId, "section=" + section, ipAddress);
  }

  @Transactional
  public int bulkArchive(String section, Set<String> publicIds, String actor, String ipAddress) {
    int changed = 0;
    for (String publicId : publicIds) {
      DataRecord record = find(section, publicId);
      record.archive();
      changed++;
    }
    audit.record(actor, "data.bulk_archive", "DataRecord", section, "count=" + changed, ipAddress);
    return changed;
  }

  private DataRecord find(String section, String publicId) {
    return records.findBySectionAndPublicIdAndDeletedAtIsNull(section, publicId)
        .orElseThrow(() -> new IllegalArgumentException("Record not found."));
  }

  public static DataRecordResponse toResponse(DataRecord record) {
    return new DataRecordResponse(
        record.getPublicId(),
        record.getSection(),
        record.getTitle(),
        record.getStatus(),
        record.getPayload(),
        record.getVersion(),
        record.getCreatedBy(),
        record.getCreatedAt(),
        record.getUpdatedAt());
  }
}
