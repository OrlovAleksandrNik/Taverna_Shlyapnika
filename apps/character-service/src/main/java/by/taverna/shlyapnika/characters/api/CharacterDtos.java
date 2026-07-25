package by.taverna.shlyapnika.characters.api;

import by.taverna.shlyapnika.characters.character.CharacterVisibility;
import by.taverna.shlyapnika.characters.rolls.RollKind;
import by.taverna.shlyapnika.characters.rolls.RollMode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CharacterDtos {
  private CharacterDtos() {
  }

  public record CharacterSheetRequest(
      @NotBlank String ownerAccountId,
      String playerPublicId,
      String gameSystem,
      String sheetType,
      CharacterVisibility visibility,
      @NotBlank String name,
      String ancestry,
      String className,
      @Min(1) @Max(30) Integer level,
      @Min(0) Integer experience,
      JsonNode payload,
      Long version
  ) {
  }

  public record CharacterSheetResponse(
      UUID id,
      String publicId,
      String ownerAccountId,
      String playerPublicId,
      String gameSystem,
      String sheetType,
      CharacterVisibility visibility,
      String name,
      String ancestry,
      String className,
      int level,
      int experience,
      JsonNode payload,
      Long version,
      Instant createdAt,
      Instant updatedAt
  ) {
  }

  public record RollRequest(
      String actorAccountId,
      RollKind kind,
      RollMode mode,
      @NotBlank String label,
      @NotBlank String formula,
      Map<String, Integer> variables
  ) {
  }

  public record RollResponse(
      UUID id,
      String publicId,
      String characterPublicId,
      String actorAccountId,
      RollKind kind,
      RollMode mode,
      String label,
      String formula,
      String resolvedFormula,
      int total,
      JsonNode detail,
      Instant createdAt
  ) {
  }

  public record FormulaPreviewRequest(
      @NotBlank String expression,
      RollMode mode,
      Map<String, Integer> variables
  ) {
  }

  public record FormulaPreviewResponse(
      String expression,
      String resolvedExpression,
      double value,
      int total,
      JsonNode detail
  ) {
  }

  public record SheetTemplateResponse(
      String gameSystem,
      String sheetType,
      JsonNode payload
  ) {
  }
}
