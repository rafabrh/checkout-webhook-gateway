-- V3: Reconcilia pairing_sessions com PairingSessionEntity

-- 0) Se existir "instance_name" no legado, padroniza para "instance"
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='instance_name'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='instance'
  ) THEN
ALTER TABLE pairing_sessions RENAME COLUMN instance_name TO instance;
END IF;
END $$;

-- 1) Colunas base (cria o que estiver faltando)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='instance'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN instance varchar(60);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='remote_jid'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN remote_jid varchar(80);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='token_hash'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN token_hash varchar(64);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='status'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN status varchar(32);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='qr_base64'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN qr_base64 text;
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='qr_url'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN qr_url varchar(1024);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='expires_at'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN expires_at timestamptz;
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='created_at'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN created_at timestamptz;
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='public' AND table_name='pairing_sessions' AND column_name='updated_at'
  ) THEN
ALTER TABLE pairing_sessions ADD COLUMN updated_at timestamptz;
END IF;
END $$;

-- 2) Ajusta tipos/tamanhos (garante contrato)
ALTER TABLE pairing_sessions
ALTER COLUMN order_id TYPE varchar(64),
  ALTER COLUMN instance TYPE varchar(60),
  ALTER COLUMN remote_jid TYPE varchar(80),
  ALTER COLUMN token_hash TYPE varchar(64),
  ALTER COLUMN status TYPE varchar(32),
  ALTER COLUMN qr_url TYPE varchar(1024);

-- 3) Backfill inteligente
-- 3.1) instance: tenta puxar de orders.instance (melhor fonte)
UPDATE pairing_sessions ps
SET instance = o.instance
    FROM orders o
WHERE ps.order_id = o.order_id
  AND (ps.instance IS NULL OR ps.instance = '');

-- 3.2) status: se estiver NULL, seta NEW (igual teu @PrePersist)
UPDATE pairing_sessions
SET status = 'NEW'
WHERE status IS NULL OR status = '';

-- 3.3) expires_at: se estiver NULL, cria janela padrão (ex: 15 min)
UPDATE pairing_sessions
SET expires_at = now() + interval '15 minutes'
WHERE expires_at IS NULL;

-- 3.4) created_at/updated_at
UPDATE pairing_sessions
SET created_at = now()
WHERE created_at IS NULL;

UPDATE pairing_sessions
SET updated_at = now()
WHERE updated_at IS NULL;

-- 4) Agora sim: NOT NULL (depois do backfill)
ALTER TABLE pairing_sessions
    ALTER COLUMN order_id SET NOT NULL,
ALTER COLUMN instance SET NOT NULL,
  ALTER COLUMN token_hash SET NOT NULL,
  ALTER COLUMN status SET NOT NULL,
  ALTER COLUMN expires_at SET NOT NULL,
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL;

-- 5) Índices com os MESMOS nomes do Entity
CREATE UNIQUE INDEX IF NOT EXISTS idx_pairing_token_hash ON pairing_sessions(token_hash);
CREATE INDEX IF NOT EXISTS idx_pairing_expires_at ON pairing_sessions(expires_at);

-- 6) (Opcional, mas eu recomendo MUITO) 1 sessão por pedido
-- evita múltiplas sessões por order_id e bugs de concorrência
CREATE UNIQUE INDEX IF NOT EXISTS uq_pairing_order_id ON pairing_sessions(order_id);