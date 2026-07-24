# Authentication

По умолчанию `PUBLIC_REGISTRATION_ENABLED=false`.

Безопасный сценарий:

1. OWNER или SUPERADMIN создаёт приглашение.
2. Backend генерирует одноразовый token и хранит только SHA-256 hash.
3. Token имеет срок действия.
4. Пользователь принимает приглашение, задаёт пароль длиной от 12 символов.
5. Email считается подтверждённым для принятого приглашения.
6. Роль назначается заранее через invitation.

Email-подсистема:

- `EmailGateway` описывает invitation, email verification, password reset и security alert.
- `MockEmailGateway` пишет dev-сообщения в backend log и не подключается к production provider.
- Invitation, password reset и email verification tokens хранятся только как SHA-256 hash.
- Password reset request возвращает одинаковый успешный ответ и не раскрывает существование email.

Первичный OWNER:

- создаётся только через bootstrap env `CONTROL_BOOTSTRAP_OWNER_EMAIL` и `CONTROL_BOOTSTRAP_TOKEN`;
- bootstrap не создаёт `admin/admin`;
- если OWNER уже существует, bootstrap не делает ничего.

Сессии:

- создаются как server-side session;
- logout удаляет session cookie;
- таблица `control_sessions` хранит metadata устройств;
- revoke-all помечает известные sessions как revoked и пишет audit;
- полная invalidation distributed sessions требует Spring Session/Redis или JDBC session store на следующем этапе.

2FA:

- TOTP secret генерируется backend и хранится encrypted через AES-GCM с ключом из `CONTROL_ENCRYPTION_KEY`;
- setup возвращает `otpauth://` URL для QR-rendering на frontend;
- confirm требует валидный TOTP code;
- backup codes генерируются один раз и хранятся только hashed;
- login принимает TOTP или unused backup code;
- OWNER/SUPERADMIN mandatory 2FA policy должна стать bootstrap/runtime policy перед production.
