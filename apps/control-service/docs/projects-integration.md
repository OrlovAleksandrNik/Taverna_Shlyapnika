# Projects Integration

Найденные managed projects:

- VoiceMod: `../../voicemod-panel-d-work`, Node.js ESM + static HTML/CSS/JS.
- ScreenStage: `../../ScreenStage-redesign`, .NET 8 WPF + LibVLCSharp.

Access assignments:

- `ProjectAssignment` links `projectCode` and `assigneePublicId`;
- assignment endpoints require `projects.configure`;
- revoke access deletes only the assignment, not the project folder;
- every assignment change is written to audit log.

В кабинете они представлены как `ManagedProject` с isolated launch contract.

Текущая политика:

- не запускать произвольный `.exe` из браузера;
- не принимать путь запуска от frontend;
- не объединять код проектов с backend;
- при `CONTROL_DESKTOP_AGENT_ENABLED=false` фиксировать mock-launch в audit;
- при `CONTROL_DESKTOP_AGENT_ENABLED=true` запускать только allowlist path из `CONTROL_DESKTOP_AGENT_VOICEMOD_PATH` или `CONTROL_DESKTOP_AGENT_SCREENSTAGE_PATH`;
- отклонять missing path, отсутствующий файл и расширения вне `.exe`, `.cmd`, `.bat`.

Следующий слой Desktop Agent должен иметь:

- локальную установку на доверенной машине;
- service-to-service authentication;
- allowlist project code -> executable path;
- подпись или checksum проверку;
- журнал запуска;
- запрет произвольных аргументов от frontend.
