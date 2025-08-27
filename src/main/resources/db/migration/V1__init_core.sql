-- Historial de mensajes (metadatos)
CREATE TABLE IF NOT EXISTS message_log (
    id               BIGSERIAL PRIMARY KEY,
    message_id       VARCHAR(64)  NOT NULL,        -- idempotencia
    channel          VARCHAR(120) NOT NULL,        -- {squad}.{topic}.{event}
    routing_key      VARCHAR(120) NOT NULL,        -- clave usada en el broker
    status           VARCHAR(24)  NOT NULL,        -- PUBLISHED / FAILED / DLQ / ...
    attempts         INT          NOT NULL DEFAULT 0,
    error_code       VARCHAR(64),                  -- opcional
    error_message    TEXT,                         -- opcional
    correlation_id   VARCHAR(64),                  -- trazabilidad cross-sistema
    produced_at      TIMESTAMPTZ  NOT NULL,        -- timestamp del productor
    published_at     TIMESTAMPTZ,                  -- cuándo salió al broker
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_message_log_message_id ON message_log (message_id);
CREATE INDEX IF NOT EXISTS ix_message_log_channel      ON message_log (channel);
CREATE INDEX IF NOT EXISTS ix_message_log_status       ON message_log (status);
CREATE INDEX IF NOT EXISTS ix_message_log_created_at   ON message_log (created_at DESC);

-- Almacenamiento del payload (JSONB) separado de los metadatos
CREATE TABLE IF NOT EXISTS payload_store (
    id            BIGSERIAL PRIMARY KEY,
    message_id    VARCHAR(64)  NOT NULL REFERENCES message_log(message_id) ON DELETE CASCADE,
    payload       JSONB        NOT NULL,
    schema_ver    VARCHAR(32),            -- versión del envelope, si aplica
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS ix_payload_store_message_id ON payload_store (message_id);
CREATE INDEX IF NOT EXISTS ix_payload_store_created_at ON payload_store (created_at DESC);
