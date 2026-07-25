package by.taverna.shlyapnika.characters.templates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

@Service
public class SheetTemplateService {
  private final ObjectMapper objectMapper;

  public SheetTemplateService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonNode dnd5e() {
    var root = objectMapper.createObjectNode();
    root.put("schemaVersion", 1);
    root.set("abilities", abilities());
    root.set("savingThrows", savingThrows());
    root.set("skills", skills());
    root.set("combat", combat());
    root.set("resources", objectMapper.createArrayNode());
    root.set("attacks", objectMapper.createArrayNode());
    root.set("features", objectMapper.createArrayNode());
    root.set("equipment", objectMapper.createArrayNode());
    root.set("spells", spells());
    root.set("personality", objectMapper.createObjectNode());
    root.set("goals", objectMapper.createArrayNode());
    root.set("notes", objectMapper.createArrayNode());
    return root;
  }

  private ObjectNode abilities() {
    var abilities = objectMapper.createObjectNode();
    ability(abilities, "strength", "STR", 10);
    ability(abilities, "dexterity", "DEX", 10);
    ability(abilities, "constitution", "CON", 10);
    ability(abilities, "intelligence", "INT", 10);
    ability(abilities, "wisdom", "WIS", 10);
    ability(abilities, "charisma", "CHA", 10);
    return abilities;
  }

  private void ability(ObjectNode parent, String key, String shortName, int score) {
    var ability = objectMapper.createObjectNode();
    ability.put("shortName", shortName);
    ability.put("score", score);
    ability.put("proficientSave", false);
    parent.set(key, ability);
  }

  private ArrayNode savingThrows() {
    return entries("strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma");
  }

  private ArrayNode skills() {
    var skills = objectMapper.createArrayNode();
    skill(skills, "athletics", "strength");
    skill(skills, "acrobatics", "dexterity");
    skill(skills, "sleightOfHand", "dexterity");
    skill(skills, "stealth", "dexterity");
    skill(skills, "arcana", "intelligence");
    skill(skills, "history", "intelligence");
    skill(skills, "investigation", "intelligence");
    skill(skills, "nature", "intelligence");
    skill(skills, "religion", "intelligence");
    skill(skills, "animalHandling", "wisdom");
    skill(skills, "insight", "wisdom");
    skill(skills, "medicine", "wisdom");
    skill(skills, "perception", "wisdom");
    skill(skills, "survival", "wisdom");
    skill(skills, "deception", "charisma");
    skill(skills, "intimidation", "charisma");
    skill(skills, "performance", "charisma");
    skill(skills, "persuasion", "charisma");
    return skills;
  }

  private void skill(ArrayNode skills, String key, String ability) {
    var skill = objectMapper.createObjectNode();
    skill.put("key", key);
    skill.put("ability", ability);
    skill.put("proficiency", "none");
    skills.add(skill);
  }

  private ObjectNode combat() {
    var combat = objectMapper.createObjectNode();
    combat.put("armorClass", 10);
    combat.put("initiativeFormula", "[DEX]");
    combat.put("speed", 30);
    combat.put("proficiencyBonus", 2);
    combat.set("hitPoints", objectMapper.createObjectNode()
        .put("current", 1)
        .put("maximum", 1)
        .put("temporary", 0));
    combat.set("deathSaves", objectMapper.createObjectNode()
        .put("successes", 0)
        .put("failures", 0));
    return combat;
  }

  private ObjectNode spells() {
    var spells = objectMapper.createObjectNode();
    spells.put("spellcastingAbility", "");
    spells.put("spellSaveDc", 0);
    spells.put("spellAttackBonus", 0);
    spells.set("slots", objectMapper.createArrayNode());
    spells.set("known", objectMapper.createArrayNode());
    return spells;
  }

  private ArrayNode entries(String... keys) {
    var array = objectMapper.createArrayNode();
    for (var key : keys) {
      var entry = objectMapper.createObjectNode();
      entry.put("key", key);
      entry.put("proficient", false);
      array.add(entry);
    }
    return array;
  }
}
