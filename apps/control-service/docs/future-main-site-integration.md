# Future Main Site Integration

Текущий этап: интеграция выключена.

Возможная synchronous схема:

```text
Control Service
-> Internal API Gateway
-> Java Backend
-> Main Site Database
```

Возможная asynchronous схема:

```text
Control Service
-> Event Bus
-> Java Backend
```

Event bus сейчас не внедряется.

Варианты:

- Synchronous REST: проще контролировать, но нужны idempotency keys и conflict handling.
- Asynchronous events: лучше для слабой связности, но нужны outbox, retries и reconciliation.
- Shared identity: единый SSO удобен, но повышает blast radius.
- Separate identity: безопаснее для первого этапа, выбран сейчас.
- Service accounts: нужны short-lived credentials и аудит.
- Idempotency: обязательна для publish/sync операций.
- Synchronization conflicts: решать version fields и explicit conflict UI.

Даже после интеграции предпочтительны отдельные базы и обмен через API/события. Прямые JPA-связи между базами не проектируются.
