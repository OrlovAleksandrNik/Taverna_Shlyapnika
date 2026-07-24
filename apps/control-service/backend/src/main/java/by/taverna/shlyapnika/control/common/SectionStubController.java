package by.taverna.shlyapnika.control.common;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

public abstract class SectionStubController {
  private final String section;

  protected SectionStubController(String section) {
    this.section = section;
  }

  @GetMapping
  public Map<String, Object> list() {
    return Map.of(
        "section", section,
        "source", "mock-control-service",
        "items", List.of(
            Map.of("id", section + "-draft-1", "status", "draft", "title", "Draft item for " + section),
            Map.of("id", section + "-published-1", "status", "published", "title", "Published item for " + section)
        ),
        "crud", List.of("list", "create-contract", "update-contract", "soft-delete-contract"),
        "note", "Contract is prepared; production data is not used.");
  }
}
