# Security

- Passwords хранятся через `BCryptPasswordEncoder` cost 12. Argon2id оставлен предпочтительным вариантом для production hardening.
- Invitation, password reset и email verification tokens хранятся только как SHA-256 hash.
- TOTP secrets шифруются AES-GCM; ключ берётся из `CONTROL_ENCRYPTION_KEY`.
- Backup codes 2FA хранятся только как password hashes.
- File validation policy blocks executable extensions and requires safe names, MIME and size bounds.
- Браузерная авторизация использует server-side session cookie `CONTROLSESSION` с `HttpOnly`, `SameSite=Strict`, production `Secure`.
- CSRF включён через `CookieCsrfTokenRepository`; исключены только login, accept invitation и public endpoints.
- CORS ограничен `CONTROL_FRONTEND_ORIGIN`; wildcard credentials не используется.
- Security headers включают CSP, frame ancestors, Permissions Policy, Referrer Policy и HSTS.
- Login throttling включён как in-memory dev limiter: 5 ошибок за 15 минут дают временную блокировку на 10 минут.
- Access tokens не кладутся в `localStorage`; frontend хранит только mock draft автосохранения.
- Все изменяющие критические backend операции пишутся в audit log.
- VoiceMod и ScreenStage не запускаются из frontend. `/projects/{code}/launch` пишет mock audit event.

Оставшиеся hardening-задачи: rate limit persistence через Redis/JDBC, mandatory 2FA policy для OWNER/SUPERADMIN, distributed session invalidation, production mail provider.
