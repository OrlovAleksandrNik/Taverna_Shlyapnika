package by.taverna.shlyapnika.characters.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SheetTemplateServiceTest {
  @Test
  void createsDnd5eTemplateForUiBootstrap() {
    var template = new SheetTemplateService(new ObjectMapper()).dnd5e();

    assertThat(template.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(template.get("abilities").get("strength").get("shortName").asText()).isEqualTo("STR");
    assertThat(template.get("skills")).hasSize(18);
    assertThat(template.get("combat").get("initiativeFormula").asText()).isEqualTo("[DEX]");
    assertThat(template.get("attacks")).isNotNull();
    assertThat(template.get("spells").get("known")).isNotNull();
  }
}
