package by.taverna.shlyapnika.characters.character;

import by.taverna.shlyapnika.characters.api.CharacterDtos.CharacterSheetRequest;
import by.taverna.shlyapnika.characters.api.CharacterDtos.CharacterSheetResponse;
import by.taverna.shlyapnika.characters.common.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterSheetService {
  private final CharacterSheetRepository sheets;
  private final ObjectMapper objectMapper;

  public CharacterSheetService(CharacterSheetRepository sheets, ObjectMapper objectMapper) {
    this.sheets = sheets;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<CharacterSheetResponse> list(String ownerAccountId) {
    if (ownerAccountId == null || ownerAccountId.isBlank()) {
      throw new IllegalArgumentException("ownerAccountId is required.");
    }
    return sheets.findTop50ByOwnerAccountIdAndDeletedAtIsNullOrderByUpdatedAtDesc(ownerAccountId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public CharacterSheet require(String publicId) {
    return sheets.findByPublicIdAndDeletedAtIsNull(publicId)
        .orElseThrow(() -> new NotFoundException("Character sheet not found."));
  }

  @Transactional(readOnly = true)
  public CharacterSheetResponse get(String publicId) {
    return toResponse(require(publicId));
  }

  @Transactional
  public CharacterSheetResponse create(CharacterSheetRequest request) {
    CharacterSheet sheet = new CharacterSheet(request.ownerAccountId(), request.name());
    applyRequest(sheet, request);
    return toResponse(sheets.save(sheet));
  }

  @Transactional
  public CharacterSheetResponse update(String publicId, CharacterSheetRequest request) {
    CharacterSheet sheet = require(publicId);
    if (request.version() != null && !request.version().equals(sheet.getVersion())) {
      throw new IllegalArgumentException("Record was changed. Reload before saving.");
    }
    applyRequest(sheet, request);
    return toResponse(sheets.save(sheet));
  }

  @Transactional
  public void delete(String publicId) {
    CharacterSheet sheet = require(publicId);
    sheet.softDelete();
    sheets.save(sheet);
  }

  public CharacterSheetResponse toResponse(CharacterSheet sheet) {
    return new CharacterSheetResponse(sheet.getId(), sheet.getPublicId(), sheet.getOwnerAccountId(),
        sheet.getPlayerPublicId(), sheet.getGameSystem(), sheet.getSheetType(), sheet.getVisibility(),
        sheet.getName(), sheet.getAncestry(), sheet.getClassName(), sheet.getLevel(), sheet.getExperience(),
        readPayload(sheet.getPayload()), sheet.getVersion(), sheet.getCreatedAt(), sheet.getUpdatedAt());
  }

  private void applyRequest(CharacterSheet sheet, CharacterSheetRequest request) {
    sheet.update(
        request.ownerAccountId(),
        blankToNull(request.playerPublicId()),
        defaultString(request.gameSystem(), "dnd5e"),
        defaultString(request.sheetType(), "dnd5e"),
        request.visibility() == null ? CharacterVisibility.PRIVATE : request.visibility(),
        request.name(),
        blankToNull(request.ancestry()),
        blankToNull(request.className()),
        request.level() == null ? 1 : request.level(),
        request.experience() == null ? 0 : request.experience(),
        writePayload(request.payload() == null || request.payload().isNull() ? objectMapper.createObjectNode() : request.payload()));
  }

  private JsonNode readPayload(String payload) {
    try {
      return objectMapper.readTree(payload);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Stored character payload is not valid JSON.", error);
    }
  }

  private String writePayload(JsonNode payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Character payload must be valid JSON.", error);
    }
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
