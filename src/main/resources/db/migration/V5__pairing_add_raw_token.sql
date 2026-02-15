ALTER TABLE pairing_sessions
    ADD COLUMN IF NOT EXISTS raw_token VARCHAR(256);
