CREATE TABLE control_users (
  id UUID PRIMARY KEY,
  public_id VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(160) NOT NULL,
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  avatar_url TEXT,
  telegram_username VARCHAR(160),
  telegram_user_id VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  timezone VARCHAR(80) NOT NULL,
  locale VARCHAR(16) NOT NULL,
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  two_factor_secret_encrypted TEXT,
  email_verified_at TIMESTAMPTZ,
  last_login_at TIMESTAMPTZ,
  deleted_at TIMESTAMPTZ,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_user_roles (
  user_id UUID NOT NULL REFERENCES control_users(id) ON DELETE CASCADE,
  role VARCHAR(48) NOT NULL,
  PRIMARY KEY (user_id, role)
);

CREATE TABLE control_invitations (
  id UUID PRIMARY KEY,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(320) NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  role VARCHAR(48) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  accepted_at TIMESTAMPTZ,
  created_by UUID REFERENCES control_users(id),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES control_users(id) ON DELETE CASCADE,
  session_hash VARCHAR(255) NOT NULL UNIQUE,
  user_agent TEXT,
  ip_address VARCHAR(96),
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_security_tokens (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES control_users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  token_type VARCHAR(48) NOT NULL,
  target_value VARCHAR(320),
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_two_factor_backup_codes (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES control_users(id) ON DELETE CASCADE,
  code_hash VARCHAR(255) NOT NULL,
  used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_login_history (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES control_users(id) ON DELETE SET NULL,
  email VARCHAR(320) NOT NULL,
  success BOOLEAN NOT NULL,
  reason VARCHAR(160),
  ip_address VARCHAR(96),
  user_agent TEXT,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_games (
  id UUID PRIMARY KEY,
  title VARCHAR(220) NOT NULL,
  description TEXT NOT NULL,
  game_system VARCHAR(120) NOT NULL,
  experience_level VARCHAR(80) NOT NULL,
  status VARCHAR(32) NOT NULL,
  master_public_id VARCHAR(64),
  starts_at TIMESTAMPTZ NOT NULL,
  duration_minutes INTEGER NOT NULL,
  min_players INTEGER NOT NULL,
  max_players INTEGER NOT NULL,
  price NUMERIC(10,2) NOT NULL,
  image_url TEXT,
  staff_notes TEXT,
  deleted_at TIMESTAMPTZ,
  published_at TIMESTAMPTZ,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_audit_log (
  id UUID PRIMARY KEY,
  actor_public_id VARCHAR(64),
  action VARCHAR(120) NOT NULL,
  entity_type VARCHAR(120) NOT NULL,
  entity_id VARCHAR(120),
  details TEXT,
  ip_address VARCHAR(96),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_managed_projects (
  id UUID PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  kind VARCHAR(80) NOT NULL,
  detected_path TEXT,
  stack TEXT,
  status VARCHAR(48) NOT NULL,
  launch_mode VARCHAR(48) NOT NULL,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_project_assignments (
  id UUID PRIMARY KEY,
  project_code VARCHAR(80) NOT NULL,
  assignee_public_id VARCHAR(64) NOT NULL,
  role_hint VARCHAR(80) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_backup_jobs (
  id UUID PRIMARY KEY,
  public_id VARCHAR(64) NOT NULL UNIQUE,
  status VARCHAR(48) NOT NULL,
  storage_path TEXT NOT NULL,
  checksum VARCHAR(128),
  created_by VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ
);

CREATE TABLE control_settings (
  key VARCHAR(160) PRIMARY KEY,
  value TEXT NOT NULL,
  sensitive BOOLEAN NOT NULL DEFAULT FALSE,
  encrypted BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_control_users_status ON control_users(status);
CREATE INDEX idx_control_security_tokens_type ON control_security_tokens(token_type, expires_at);
CREATE INDEX idx_control_games_status_starts ON control_games(status, starts_at);
CREATE INDEX idx_control_audit_created ON control_audit_log(created_at);
CREATE INDEX idx_control_login_history_email_created ON control_login_history(email, created_at);
