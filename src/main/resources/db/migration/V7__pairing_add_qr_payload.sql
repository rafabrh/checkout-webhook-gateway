ALTER TABLE pairing_session
    ADD COLUMN qr_payload TEXT NULL,
  ADD COLUMN pairing_code VARCHAR(128) NULL;