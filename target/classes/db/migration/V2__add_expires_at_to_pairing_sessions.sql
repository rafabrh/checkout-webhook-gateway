ALTER TABLE pairing_sessions
    ADD COLUMN IF NOT EXISTS expires_at timestamptz;