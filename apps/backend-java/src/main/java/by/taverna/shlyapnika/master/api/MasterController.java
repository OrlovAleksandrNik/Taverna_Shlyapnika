package by.taverna.shlyapnika.master.api;

import by.taverna.shlyapnika.master.domain.MasterEntity;
import by.taverna.shlyapnika.master.infrastructure.MasterRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MasterController {
  private final MasterRepository repository;

  public MasterController(MasterRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/api/masters")
  public MastersResponse masters() {
    return new MastersResponse(repository.findActiveMasters().stream().map(MasterDto::from).toList());
  }

  public record MastersResponse(List<MasterDto> masters) {
  }

  public record MasterDto(String id, String displayName, String contactUrl) {
    static MasterDto from(MasterEntity master) {
      return new MasterDto(master.getId(), master.getDisplayName(), master.getContactUrl());
    }
  }
}
