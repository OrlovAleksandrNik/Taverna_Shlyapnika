package by.taverna.shlyapnika.control.settings.infrastructure;

import by.taverna.shlyapnika.control.settings.domain.ControlSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlSettingRepository extends JpaRepository<ControlSetting, String> {
}
