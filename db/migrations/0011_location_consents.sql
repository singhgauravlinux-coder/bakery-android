-- Consent decisions (starting with location access), one row per user per
-- consent type. Idempotent; also applied automatically by consent-service
-- on startup (see services/consent-service/server.js MIGRATION).
CREATE TABLE IF NOT EXISTS user_consents (
  user_id       TEXT NOT NULL,
  consent_type  TEXT NOT NULL,
  granted       BOOLEAN NOT NULL,
  source        TEXT NOT NULL DEFAULT 'app',
  responded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, consent_type)
);
CREATE INDEX IF NOT EXISTS idx_user_consents_type ON user_consents (consent_type);
