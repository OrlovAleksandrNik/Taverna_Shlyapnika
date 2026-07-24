# Projects Integration

Найденные managed projects:

- VoiceMod: `../../voicemod-panel-d-work`, Node.js ESM + static HTML/CSS/JS.
- ScreenStage: `../../ScreenStage-redesign`, .NET 8 WPF + LibVLCSharp.

В кабинете они представлены как `ManagedProject` с mock status.

Текущая политика:

- не запускать `.exe` из браузера;
- не принимать путь запуска от frontend;
- не объединять код проектов с backend;
- фиксировать mock-launch в audit;
- реальные действия только через будущий Desktop Agent.

Desktop Agent должен иметь:

- локальную установку на доверенной машине;
- service-to-service authentication;
- allowlist project code -> executable path;
- подпись или checksum проверку;
- журнал запуска;
- запрет произвольных аргументов от frontend.
