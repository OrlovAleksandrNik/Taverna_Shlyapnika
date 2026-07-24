package by.taverna.shlyapnika.control.common;

import by.taverna.shlyapnika.control.common.DataRecordDtos.BulkActionRequest;
import by.taverna.shlyapnika.control.common.DataRecordDtos.DataRecordRequest;
import by.taverna.shlyapnika.control.common.DataRecordDtos.DataRecordResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/data/{section}")
public class DataRecordsController {
  private final DataRecordService service;
  private final SectionPermissionPolicy permissions;

  public DataRecordsController(DataRecordService service, SectionPermissionPolicy permissions) {
    this.service = service;
    this.permissions = permissions;
  }

  @GetMapping
  Page<DataRecordResponse> list(
      @PathVariable String section,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    permissions.requireRead(section, authentication);
    return service.list(section, q, page, size);
  }

  @PostMapping
  DataRecordResponse create(@PathVariable String section, @Valid @RequestBody DataRecordRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    permissions.requireWrite(section, authentication);
    return service.create(section, request, authentication.getName(), servletRequest.getRemoteAddr());
  }

  @PutMapping("/{publicId}")
  DataRecordResponse update(@PathVariable String section, @PathVariable String publicId, @Valid @RequestBody DataRecordRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    permissions.requireWrite(section, authentication);
    return service.update(section, publicId, request, authentication.getName(), servletRequest.getRemoteAddr());
  }

  @PostMapping("/{publicId}/publish")
  DataRecordResponse publish(@PathVariable String section, @PathVariable String publicId, Authentication authentication, HttpServletRequest servletRequest) {
    permissions.requirePublish(section, authentication);
    return service.publish(section, publicId, authentication.getName(), servletRequest.getRemoteAddr());
  }

  @DeleteMapping("/{publicId}")
  void softDelete(@PathVariable String section, @PathVariable String publicId, Authentication authentication, HttpServletRequest servletRequest) {
    permissions.requireWrite(section, authentication);
    service.softDelete(section, publicId, authentication.getName(), servletRequest.getRemoteAddr());
  }

  @PostMapping("/bulk/archive")
  Map<String, Integer> bulkArchive(@PathVariable String section, @Valid @RequestBody BulkActionRequest request, Authentication authentication, HttpServletRequest servletRequest) {
    permissions.requireWrite(section, authentication);
    return Map.of("archived", service.bulkArchive(section, request.publicIds(), authentication.getName(), servletRequest.getRemoteAddr()));
  }
}
