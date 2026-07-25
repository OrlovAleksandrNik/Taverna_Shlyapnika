package by.taverna.shlyapnika.characters.api;

import by.taverna.shlyapnika.characters.api.CharacterDtos.CharacterSheetRequest;
import by.taverna.shlyapnika.characters.api.CharacterDtos.CharacterSheetResponse;
import by.taverna.shlyapnika.characters.api.CharacterDtos.FormulaPreviewRequest;
import by.taverna.shlyapnika.characters.api.CharacterDtos.FormulaPreviewResponse;
import by.taverna.shlyapnika.characters.api.CharacterDtos.RollRequest;
import by.taverna.shlyapnika.characters.api.CharacterDtos.RollResponse;
import by.taverna.shlyapnika.characters.api.CharacterDtos.SheetTemplateResponse;
import by.taverna.shlyapnika.characters.character.CharacterSheetService;
import by.taverna.shlyapnika.characters.rolls.RollService;
import by.taverna.shlyapnika.characters.templates.SheetTemplateService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1")
public class CharactersController {
  private final CharacterSheetService sheets;
  private final RollService rolls;
  private final SheetTemplateService templates;

  public CharactersController(CharacterSheetService sheets, RollService rolls, SheetTemplateService templates) {
    this.sheets = sheets;
    this.rolls = rolls;
    this.templates = templates;
  }

  @GetMapping("/characters")
  List<CharacterSheetResponse> list(@RequestParam String ownerAccountId) {
    return sheets.list(ownerAccountId);
  }

  @PostMapping("/characters")
  CharacterSheetResponse create(@Valid @RequestBody CharacterSheetRequest request) {
    return sheets.create(request);
  }

  @GetMapping("/characters/{publicId}")
  CharacterSheetResponse get(@PathVariable String publicId) {
    return sheets.get(publicId);
  }

  @PutMapping("/characters/{publicId}")
  CharacterSheetResponse update(@PathVariable String publicId, @Valid @RequestBody CharacterSheetRequest request) {
    return sheets.update(publicId, request);
  }

  @DeleteMapping("/characters/{publicId}")
  void delete(@PathVariable String publicId) {
    sheets.delete(publicId);
  }

  @PostMapping("/characters/{publicId}/rolls")
  RollResponse roll(@PathVariable String publicId, @Valid @RequestBody RollRequest request) {
    return rolls.roll(publicId, request);
  }

  @GetMapping("/characters/{publicId}/rolls")
  List<RollResponse> rolls(@PathVariable String publicId) {
    return rolls.list(publicId);
  }

  @PostMapping("/formulas/preview")
  FormulaPreviewResponse preview(@Valid @RequestBody FormulaPreviewRequest request) {
    return rolls.preview(request.expression(), request.mode(), request.variables());
  }

  @GetMapping("/sheet-templates/dnd5e")
  SheetTemplateResponse dnd5eTemplate() {
    return new SheetTemplateResponse("dnd5e", "dnd5e", templates.dnd5e());
  }
}
