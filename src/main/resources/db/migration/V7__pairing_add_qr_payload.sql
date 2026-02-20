ALTER TABLE pairing_sessions
    ADD COLUMN qr_payload TEXT NULL,
  ADD COLUMN pairing_code VARCHAR(128) NULL;