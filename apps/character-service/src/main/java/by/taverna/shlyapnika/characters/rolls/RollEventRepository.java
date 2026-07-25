package by.taverna.shlyapnika.characters.rolls;

import by.taverna.shlyapnika.characters.character.CharacterSheet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RollEventRepository extends JpaRepository<RollEvent, UUID> {
  List<RollEvent> findTop50ByCharacterOrderByCreatedAtDesc(CharacterSheet character);
}
