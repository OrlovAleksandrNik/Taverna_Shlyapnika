package by.taverna.shlyapnika.characters.rolls;

import by.taverna.shlyapnika.characters.api.CharacterDtos.FormulaPreviewResponse;
import by.taverna.shlyapnika.characters.api.CharacterDtos.RollRequest;
import by.taverna.shlyapnika.characters.api.CharacterDtos.RollResponse;
import by.taverna.shlyapnika.characters.character.CharacterSheet;
import by.taverna.shlyapnika.characters.character.CharacterSheetService;
import by.taverna.shlyapnika.characters.mechanics.FormulaEngine;
import by.taverna.shlyapnika.characters.mechanics.FormulaEvaluation;
import by.taverna.shlyapnika.characters.mechanics.RandomDiceRoller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RollService {
  private final CharacterSheetService sheets;
  private final RollEventRepository rolls;
  private final FormulaEngine formulaEngine;
  private final RandomDiceRoller diceRoller;
  private final ObjectMapper objectMapper;

  public RollService(CharacterSheetService sheets, RollEventRepository rolls, FormulaEngine formulaEngine,
      RandomDiceRoller diceRoller, ObjectMapper objectMapper) {
    this.sheets = sheets;
    this.rolls = rolls;
    this.formulaEngine = formulaEngine;
    this.diceRoller = diceRoller;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public RollResponse roll(String characterPublicId, RollRequest request) {
    CharacterSheet sheet = sheets.require(characterPublicId);
    RollMode mode = request.mode() == null ? RollMode.NORMAL : request.mode();
    RollKind kind = request.kind() == null ? RollKind.CUSTOM : request.kind();
    FormulaEvaluation evaluation = formulaEngine.evaluate(request.formula(), safeVariables(request.variables()), mode, diceRoller);
    RollEvent event = new RollEvent(sheet, request.actorAccountId(), kind, mode, request.label(), request.formula(),
        evaluation.resolvedExpression(), evaluation.intTotal(), writeJson(evaluation));
    return toResponse(rolls.save(event));
  }

  @Transactional(readOnly = true)
  public List<RollResponse> list(String characterPublicId) {
    CharacterSheet sheet = sheets.require(characterPublicId);
    return rolls.findTop50ByCharacterOrderByCreatedAtDesc(sheet).stream().map(this::toResponse).toList();
  }

  public FormulaPreviewResponse preview(String expression, RollMode mode, Map<String, Integer> variables) {
    FormulaEvaluation evaluation = formulaEngine.evaluate(expression, safeVariables(variables),
        mode == null ? RollMode.NORMAL : mode, diceRoller);
    return new FormulaPreviewResponse(evaluation.expression(), evaluation.resolvedExpression(), evaluation.value(),
        evaluation.intTotal(), objectMapper.valueToTree(evaluation));
  }

  private RollResponse toResponse(RollEvent event) {
    return new RollResponse(event.getId(), event.getPublicId(), event.getCharacter().getPublicId(),
        event.getActorAccountId(), event.getKind(), event.getMode(), event.getLabel(), event.getFormula(),
        event.getResolvedFormula(), event.getTotal(), readJson(event.getDetailJson()), event.getCreatedAt());
  }

  private Map<String, Integer> safeVariables(Map<String, Integer> variables) {
    return variables == null ? Map.of() : variables;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Roll detail could not be serialized.", error);
    }
  }

  private JsonNode readJson(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Stored roll detail is not valid JSON.", error);
    }
  }
}
