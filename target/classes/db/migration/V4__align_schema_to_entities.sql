
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS orders (
                                      id            BIGSERIAL PRIMARY KEY,
                                      order_id      VARCHAR(64)  NOT NULL UNIQUE,
    plan          VARCHAR(64)  NOT NULL,
    channel       VARCHAR(32)  NOT NULL,
    instance      VARCHAR(64),
    remote_jid    VARCHAR(64)  NOT NULL,
    customer_name VARCHAR(140),
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

ALTER TABLE orders ADD COLUMN IF NOT EXISTS plan          VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS channel       VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS instance      VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS remote_jid    VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(140);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS status        VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS created_at    TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMPTZ;

ALTER TABLE orders
ALTER COLUMN plan          TYPE VARCHAR(64),
  ALTER COLUMN channel       TYPE VARCHAR(32),
  ALTER COLUMN instance      TYPE VARCHAR(64),
  ALTER COLUMN remote_jid    TYPE VARCHAR(64),
  ALTER COLUMN customer_name TYPE VARCHAR(140),
  ALTER COLUMN status        TYPE VARCHAR(32);

UPDATE orders SET created_at = now() WHERE created_at IS NULL;
UPDATE orders SET updated_at = now() WHERE updated_at IS NULL;

DO $do$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema='public' AND table_name='pairing_sessions'
  ) THEN
    EXECUTE $sql$
UPDATE orders o
SET remote_jid = ps.remote_jid
    FROM pairing_sessions ps
WHERE o.order_id = ps.order_id
  AND (o.remote_jid IS NULL OR o.remote_jid = '')
  AND ps.remote_jid IS NOT NULL
  AND ps.remote_jid <> ''
    $sql$;
END IF;
END
$do$;

DO $do$
BEGIN
  IF EXISTS (SELECT 1 FROM orders WHERE remote_jid IS NULL OR remote_jid = '') THEN
    RAISE EXCEPTION 'V4: orders.remote_jid contém NULL/vazio. Corrija/backfill antes do NOT NULL.';
END IF;
END
$do$;

ALTER TABLE orders
    ALTER COLUMN order_id   SET NOT NULL,
ALTER COLUMN plan       SET NOT NULL,
  ALTER COLUMN channel    SET NOT NULL,
  ALTER COLUMN remote_jid SET NOT NULL,
  ALTER COLUMN status     SET NOT NULL,
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL;

DO $do$
BEGIN
  IF EXISTS (SELECT 1 FROM orders WHERE status NOT IN ('CREATED','PAID','PROVISIONED','CANCELED')) THEN
    RAISE EXCEPTION 'V4: orders.status contém valores fora do enum OrderStatus.';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema='public' AND table_name='orders' AND constraint_name='ck_orders_status'
  ) THEN
ALTER TABLE orders
    ADD CONSTRAINT ck_orders_status
        CHECK (status IN ('CREATED','PAID','PROVISIONED','CANCELED'));
END IF;
END
$do$;

CREATE TABLE IF NOT EXISTS payments (
                                        id         BIGSERIAL PRIMARY KEY,
                                        payment_id VARCHAR(64)   NOT NULL UNIQUE,
    order_id   VARCHAR(64)   NOT NULL,
    status     VARCHAR(32)   NOT NULL,
    amount     NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_payments_order_id ON payments(order_id);

CREATE TABLE IF NOT EXISTS pairing_sessions (
                                                id         BIGSERIAL PRIMARY KEY,
                                                order_id   VARCHAR(64)  NOT NULL,
    instance   VARCHAR(60)  NOT NULL,
    remote_jid VARCHAR(80),
    token_hash VARCHAR(64)  NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    qr_base64  TEXT,
    qr_url     VARCHAR(1024),
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

DO $do$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='instance_name'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='instance'
  ) THEN
    EXECUTE 'ALTER TABLE pairing_sessions RENAME COLUMN instance_name TO instance';
END IF;
END
$do$;

ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS order_id   VARCHAR(64);
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS instance   VARCHAR(60);
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS remote_jid VARCHAR(80);
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS status     VARCHAR(32);
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS qr_base64  TEXT;
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS qr_url     VARCHAR(1024);
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE pairing_sessions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

DO $do$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='token'
  ) THEN
    EXECUTE $sql$
UPDATE pairing_sessions
SET token_hash = encode(digest(token, 'sha256'), 'hex')
WHERE (token_hash IS NULL OR token_hash = '')
  AND token IS NOT NULL AND token <> ''
    $sql$;

EXECUTE 'ALTER TABLE pairing_sessions DROP COLUMN token';
END IF;
END
$do$;

ALTER TABLE pairing_sessions
ALTER COLUMN order_id   TYPE VARCHAR(64),
  ALTER COLUMN instance   TYPE VARCHAR(60),
  ALTER COLUMN remote_jid TYPE VARCHAR(80),
  ALTER COLUMN token_hash TYPE VARCHAR(64),
  ALTER COLUMN status     TYPE VARCHAR(32),
  ALTER COLUMN qr_url     TYPE VARCHAR(1024);

UPDATE pairing_sessions ps
SET instance = o.instance
    FROM orders o
WHERE ps.order_id = o.order_id
  AND (ps.instance IS NULL OR ps.instance = '');

UPDATE pairing_sessions
SET status = 'NEW'
WHERE status IS NULL OR status = '';

UPDATE pairing_sessions
SET expires_at = now() + interval '15 minutes'
WHERE expires_at IS NULL;

UPDATE pairing_sessions SET created_at = now() WHERE created_at IS NULL;
UPDATE pairing_sessions SET updated_at = now() WHERE updated_at IS NULL;

DO $do$
BEGIN
  IF EXISTS (SELECT 1 FROM pairing_sessions WHERE order_id IS NULL OR order_id = '') THEN
    RAISE EXCEPTION 'V4: pairing_sessions.order_id contém NULL/vazio.';
END IF;

  IF EXISTS (SELECT 1 FROM pairing_sessions WHERE instance IS NULL OR instance = '') THEN
    RAISE EXCEPTION 'V4: pairing_sessions.instance contém NULL/vazio.';
END IF;

  IF EXISTS (SELECT 1 FROM pairing_sessions WHERE token_hash IS NULL OR token_hash = '') THEN
    RAISE EXCEPTION 'V4: pairing_sessions.token_hash contém NULL/vazio.';
END IF;

  IF EXISTS (SELECT 1 FROM pairing_sessions WHERE status IS NULL OR status = '') THEN
    RAISE EXCEPTION 'V4: pairing_sessions.status contém NULL/vazio.';
END IF;

  IF EXISTS (SELECT 1 FROM pairing_sessions WHERE expires_at IS NULL) THEN
    RAISE EXCEPTION 'V4: pairing_sessions.expires_at contém NULL.';
END IF;
END
$do$;

ALTER TABLE pairing_sessions
    ALTER COLUMN order_id   SET NOT NULL,
ALTER COLUMN instance   SET NOT NULL,
  ALTER COLUMN token_hash SET NOT NULL,
  ALTER COLUMN status     SET NOT NULL,
  ALTER COLUMN expires_at SET NOT NULL,
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL;

DO $do$
BEGIN
  IF EXISTS (
    SELECT 1
      FROM pairing_sessions
     GROUP BY token_hash
    HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION 'V4: pairing_sessions tem token_hash duplicado.';
END IF;
END
$do$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_pairing_token_hash ON pairing_sessions(token_hash);
CREATE INDEX IF NOT EXISTS idx_pairing_expires_at ON pairing_sessions(expires_at);

DO $do$
BEGIN
  IF EXISTS (
    SELECT 1
      FROM payments p
     WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = p.order_id)
  ) THEN
    RAISE EXCEPTION 'V4: payments órfãos (order_id sem orders).';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema='public' AND table_name='payments' AND constraint_name='fk_payments_order_id'
  ) THEN
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_order_id
        FOREIGN KEY (order_id) REFERENCES orders(order_id);
END IF;

  IF EXISTS (
    SELECT 1
      FROM pairing_sessions ps
     WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = ps.order_id)
  ) THEN
    RAISE EXCEPTION 'V4: pairing_sessions órfãs (order_id sem orders).';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_schema='public' AND table_name='pairing_sessions' AND constraint_name='fk_pairing_sessions_order_id'
  ) THEN
ALTER TABLE pairing_sessions
    ADD CONSTRAINT fk_pairing_sessions_order_id
        FOREIGN KEY (order_id) REFERENCES orders(order_id);
END IF;
END
$do$;