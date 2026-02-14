ALTER TABLE pairing_session
    ADD COLUMN IF NOT EXISTS raw_token VARCHAR(256);

CREATE INDEX IF NOT EXISTS idx_pairing_order_created
    ON pairing_session (order_id, created_at DESC);
