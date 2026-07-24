# Security

- Passwords хранятся через `BCryptPasswordEncoder` cost 12. Argon2id оставлен предпочтительным вариантом для production hardening.
- Браузерная авторизация использует server-side session cookie `CONTROLSESSION` с `HttpOnly`, `SameSite=Strict`, production `Secure`.
- CSRF включён через `CookieCsrfTokenRepository`; исключены только login, accept invitation и public endpoints.
- CORS ограничен `CONTROL_FRONTEND_ORIGIN`; wildcard credentials не используется.
- Security headers включают CSP, frame ancestors, Permissions Policy, Referrer Policy и HSTS.
- Access tokens не кладутся в `localStorage`; frontend хранит только mock draft автосохранения.
- Все изменяющие критические backend операции пишутся в audit log.
- VoiceMod и ScreenStage не запускаются из frontend. `/projects/{code}/launch` пишет mock audit event.

2FA/TOTP контуры описаны в authentication docs. Реальное шифрование TOTP secrets и backup codes должно быть включено перед production.
