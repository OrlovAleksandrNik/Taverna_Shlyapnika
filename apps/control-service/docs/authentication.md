# Authentication

По умолчанию `PUBLIC_REGISTRATION_ENABLED=false`.

Безопасный сценарий:

1. OWNER или SUPERADMIN создаёт приглашение.
2. Backend генерирует одноразовый token и хранит только SHA-256 hash.
3. Token имеет срок действия.
4. Пользователь принимает приглашение, задаёт пароль длиной от 12 символов.
5. Email считается подтверждённым для принятого приглашения; отдельный email verification gateway подготовлен как следующий шаг.
6. Роль назначается заранее через invitation.

Первичный OWNER:

- создаётся только через bootstrap env `CONTROL_BOOTSTRAP_OWNER_EMAIL` и `CONTROL_BOOTSTRAP_TOKEN`;
- bootstrap не создаёт `admin/admin`;
- если OWNER уже существует, bootstrap не делает ничего.

Сессии:

- создаются как server-side session;
- logout удаляет session cookie;
- таблица `control_sessions` предусмотрена для device management и принудительного logout.

2FA:

- модель пользователя содержит `twoFactorEnabled` и encrypted secret field;
- OWNER/SUPERADMIN должны получить mandatory 2FA в production hardening;
- backup codes должны храниться только в hashed виде.
