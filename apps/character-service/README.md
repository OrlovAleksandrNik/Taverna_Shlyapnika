# Taverna Character Service

Independent Spring Boot microservice for character-sheet storage and mechanical rolls.

## What is included

- Flyway-owned PostgreSQL schema.
- Character sheet CRUD with an opaque JSON payload for UI-specific sheet data.
- External account linkage via `ownerAccountId` and optional `playerPublicId`.
- Formula engine for variables such as `[STR]`, `[DEX]`, `[PROF]`, `[LVL]`.
- Dice expressions such as `d20 + 7` and `1d6 + [STR]`.
- Advantage/disadvantage mode for single `d20` terms.
- Roll event history per character.
- D&D 5e sheet template endpoint for bootstrapping the future UI.
- Optional API token guard for public deployments.

## Run locally

From the repository root:

```powershell
pnpm run dev:character:java
```

By default the service listens on `http://localhost:4192` and expects PostgreSQL on
`jdbc:postgresql://localhost:5435/taverna_characters`.

To start the isolated database and service with Docker:

```powershell
docker compose --profile character up --build character-service
```

Swagger UI is available at:

```text
http://localhost:4192/swagger-ui/index.html
```

## Railway

Create a separate Railway service from this repository and set its root directory to:

```text
apps/character-service
```

The service has its own `railway.json` and `Dockerfile` when the Railway service root directory is set to
`apps/character-service`. If the service is connected from the monorepo root, set `RAILWAY_DOCKERFILE_PATH` to
`apps/character-service/Dockerfile.railway`.

Attach a Railway PostgreSQL database to the service; Railway's standard `DATABASE_URL` variable is enough.
`CHARACTER_DATABASE_URL` and `CHARACTER_DATABASE_JDBC_URL` can be used as explicit overrides.

Recommended Railway variables:

```text
SPRING_PROFILES_ACTIVE=prod
RAILWAY_DOCKERFILE_PATH=apps/character-service/Dockerfile.railway
CHARACTER_CORS_ALLOWED_ORIGINS=https://your-site.example
CHARACTER_API_TOKEN=<secret>
CHARACTER_SWAGGER_UI_ENABLED=false
```

Health endpoints:

```text
GET /health
GET /ready
```

## Example requests

Create a character:

```http
POST /api/v1/characters
Content-Type: application/json

{
  "ownerAccountId": "acct_demo",
  "playerPublicId": "player_demo",
  "name": "Alkis",
  "ancestry": "Human",
  "className": "Monk",
  "level": 7,
  "payload": {
    "abilities": {
      "STR": 12,
      "DEX": 18,
      "CON": 14,
      "INT": 12,
      "WIS": 16,
      "CHA": 12
    }
  }
}
```

Preview a formula:

```http
POST /api/v1/formulas/preview
Content-Type: application/json

{
  "expression": "d20 + [PROF] + [DEX]",
  "mode": "ADVANTAGE",
  "variables": {
    "PROF": 3,
    "DEX": 4
  }
}
```

Fetch the default D&D 5e payload shape:

```http
GET /api/v1/sheet-templates/dnd5e
Authorization: Bearer <CHARACTER_API_TOKEN>
```
