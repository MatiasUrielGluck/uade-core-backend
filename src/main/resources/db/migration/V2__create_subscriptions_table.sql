-- Migración para crear la tabla de suscripciones
-- V2__create_subscriptions_table.sql

CREATE TABLE subscriptions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    webhook_url VARCHAR(500) NOT NULL,
    squad_name VARCHAR(100) NOT NULL,
    topic VARCHAR(200) NOT NULL,
    event_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    last_successful_delivery TIMESTAMP WITH TIME ZONE,
    
    -- Índices para mejorar el rendimiento de las consultas
    CONSTRAINT uk_webhook_topic UNIQUE (webhook_url, topic),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_failed_attempts CHECK (failed_attempts >= 0)
);

-- Índices para optimizar las consultas más comunes
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_squad_name ON subscriptions(squad_name);
CREATE INDEX idx_subscriptions_topic ON subscriptions(topic);
CREATE INDEX idx_subscriptions_event_name ON subscriptions(event_name);
CREATE INDEX idx_subscriptions_created_at ON subscriptions(created_at);

-- Comentarios para documentar la tabla
COMMENT ON TABLE subscriptions IS 'Tabla para almacenar las suscripciones a tópicos de mensajería';
COMMENT ON COLUMN subscriptions.id IS 'Identificador único de la suscripción (UUID)';
COMMENT ON COLUMN subscriptions.webhook_url IS 'URL del webhook donde se enviarán las notificaciones';
COMMENT ON COLUMN subscriptions.squad_name IS 'Nombre del squad que se suscribe al tópico (soporta wildcards * y #)';
COMMENT ON COLUMN subscriptions.topic IS 'Tópico al que se suscribe (soporta wildcards * y #)';
COMMENT ON COLUMN subscriptions.event_name IS 'Evento específico al que se suscribe (soporta wildcards * y #)';
COMMENT ON COLUMN subscriptions.status IS 'Estado de la suscripción: ACTIVE, INACTIVE, SUSPENDED';
COMMENT ON COLUMN subscriptions.created_at IS 'Fecha y hora de creación de la suscripción';
COMMENT ON COLUMN subscriptions.updated_at IS 'Fecha y hora de la última actualización de la suscripción';
COMMENT ON COLUMN subscriptions.failed_attempts IS 'Número de intentos fallidos de entrega de webhook';
COMMENT ON COLUMN subscriptions.last_error IS 'Último error de entrega del webhook';
COMMENT ON COLUMN subscriptions.last_successful_delivery IS 'Fecha y hora de la última entrega exitosa';
