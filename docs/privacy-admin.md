# Отзыв согласия и удаление данных

Минимальный административный сценарий подготовлен через backend-метод:

```http
POST /api/internal/privacy/withdraw-consent
X-Internal-Token: <INTERNAL_API_TOKEN>
Content-Type: application/json

{
  "entityType": "GameSignup",
  "requestId": "ID заявки",
  "anonymize": true
}
```

`entityType` может быть:

- `GameSignup` — запись игрока на игру;
- `ServiceRequest` — заявка на услугу;
- `ContactRequest` — сообщение из формы обратной связи.

Если `anonymize: true`, контактные поля заменяются технической пометкой `withdrawn:<ID>`, комментарии удаляются или обезличиваются, а в записи сохраняются даты `consentWithdrawnAt`, `dataUseStoppedAt` и `anonymizedAt`.

Если `anonymize: false`, заявка только отмечается как отозванная, чтобы администратор мог вручную решить вопрос хранения по законному основанию.

Не передавайте `INTERNAL_API_TOKEN` в браузер, публичный JavaScript, логи или Telegram-сообщения.
