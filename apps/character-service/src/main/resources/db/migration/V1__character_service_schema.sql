CREATE TABLE character_sheets (
  id UUID PRIMARY KEY,
  public_id VARCHAR(64) NOT NULL UNIQUE,
  owner_account_id VARCHAR(64) NOT NULL,
  player_public_id VARCHAR(64),
  game_system VARCHAR(64) NOT NULL,
  sheet_type VARCHAR(64) NOT NULL,
  visibility VARCHAR(32) NOT NULL,
  name VARCHAR(160) NOT NULL,
  ancestry VARCHAR(120),
  class_name VARCHAR(160),
  level INTEGER NOT NULL,
  experience INTEGER NOT NULL DEFAULT 0,
  payload TEXT NOT NULL,
  deleted_at TIMESTAMPTZ,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE character_roll_events (
  id UUID PRIMARY KEY,
  public_id VARCHAR(64) NOT NULL UNIQUE,
  character_id UUID NOT NULL REFERENCES character_sheets(id) ON DELETE CASCADE,
  actor_account_id VARCHAR(64),
  kind VARCHAR(48) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  label VARCHAR(160) NOT NULL,
  formula VARCHAR(240) NOT NULL,
  resolved_formula VARCHAR(320) NOT NULL,
  total INTEGER NOT NULL,
  detail_json TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_character_sheets_owner ON character_sheets(owner_account_id, deleted_at, updated_at);
CREATE INDEX idx_character_sheets_player ON character_sheets(player_public_id);
CREATE INDEX idx_character_roll_events_character_created ON character_roll_events(character_id, created_at DESC);
