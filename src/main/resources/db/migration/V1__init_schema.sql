-- ============================================================
-- V1__init_schema.sql
-- SaaS Gestión de Turnos — Esquema inicial
-- ============================================================

-- =====================
-- EXTENSIONS
-- =====================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================
-- TIPOS ENUM
-- =====================
CREATE TYPE turno_estado AS ENUM (
    'SOLICITADO',
    'EN_COTIZACION',
    'COTIZADO',
    'CONFIRMADO',
    'PROGRAMADO',
    'FINALIZADO',
    'CANCELADO'
);

CREATE TYPE rol_enum AS ENUM (
    'CLIENTE',
    'OPERADOR',
    'ADMIN'
);

CREATE TYPE cotizacion_estado AS ENUM (
    'PENDIENTE',
    'ACEPTADA',
    'RECHAZADA'
);

-- =====================
-- FUNCIÓN updated_at
-- =====================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================
-- TABLA: empresas
-- =====================
CREATE TABLE empresas (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre                   VARCHAR(150) NOT NULL,
    email_contacto           VARCHAR(150) NOT NULL,
    direccion                VARCHAR(255),
    telefono                 VARCHAR(30),
    hora_apertura            TIME         NOT NULL DEFAULT '08:00',
    hora_cierre              TIME         NOT NULL DEFAULT '18:00',
    duracion_slot_minutos    INTEGER      NOT NULL DEFAULT 30 CHECK (duracion_slot_minutos > 0),
    sabado_habilitado        BOOLEAN      NOT NULL DEFAULT FALSE,
    domingo_habilitado       BOOLEAN      NOT NULL DEFAULT FALSE,
    activa                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_empresas_updated_at
    BEFORE UPDATE ON empresas
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================
-- TABLA: usuarios
-- =====================
CREATE TABLE usuarios (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID         REFERENCES empresas(id) ON DELETE SET NULL,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    telefono       VARCHAR(30),
    rol            rol_enum     NOT NULL DEFAULT 'CLIENTE',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================
-- TABLA: refresh_tokens
-- =====================
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    tipo        VARCHAR(20)  NOT NULL DEFAULT 'REFRESH',
    expira_en   TIMESTAMPTZ  NOT NULL,
    revocado    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =====================
-- TABLA: servicios
-- =====================
CREATE TABLE servicios (
    id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id                UUID          NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    nombre                    VARCHAR(150)  NOT NULL,
    descripcion               TEXT,
    precio_base               NUMERIC(12,2) NOT NULL CHECK (precio_base >= 0),
    duracion_estimada_minutos INTEGER       NOT NULL CHECK (duracion_estimada_minutos > 0),
    activo                    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_servicios_updated_at
    BEFORE UPDATE ON servicios
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================
-- TABLA: turnos
-- =====================
CREATE TABLE turnos (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID          NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    cliente_id        UUID          NOT NULL REFERENCES usuarios(id),
    servicio_id       UUID          REFERENCES servicios(id) ON DELETE SET NULL,
    fecha_solicitada  DATE,
    hora_solicitada   TIME,
    fecha_confirmada  DATE,
    hora_confirmada   TIME,
    estado            turno_estado  NOT NULL DEFAULT 'SOLICITADO',
    observaciones     TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_turnos_updated_at
    BEFORE UPDATE ON turnos
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================
-- TABLA: turno_historial
-- =====================
CREATE TABLE turno_historial (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    turno_id         UUID         NOT NULL REFERENCES turnos(id) ON DELETE CASCADE,
    estado_anterior  turno_estado,
    estado_nuevo     turno_estado NOT NULL,
    motivo           TEXT,
    cambiado_por_id  UUID         REFERENCES usuarios(id) ON DELETE SET NULL,
    timestamp        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =====================
-- TABLA: cotizaciones
-- =====================
CREATE TABLE cotizaciones (
    id                UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    turno_id          UUID              NOT NULL UNIQUE REFERENCES turnos(id) ON DELETE CASCADE,
    servicio_id       UUID              REFERENCES servicios(id) ON DELETE SET NULL,
    precio            NUMERIC(12,2)     NOT NULL CHECK (precio >= 0),
    duracion_minutos  INTEGER           NOT NULL CHECK (duracion_minutos > 0),
    descripcion       TEXT,
    estado            cotizacion_estado NOT NULL DEFAULT 'PENDIENTE',
    created_at        TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_cotizaciones_updated_at
    BEFORE UPDATE ON cotizaciones
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================
-- TABLA: senias
-- =====================
CREATE TABLE senias (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    turno_id      UUID          NOT NULL UNIQUE REFERENCES turnos(id) ON DELETE CASCADE,
    monto         NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    metodo_pago   VARCHAR(50)   NOT NULL,
    referencia    VARCHAR(200),
    registrado_en TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- =====================
-- TABLA: bloqueos_agenda
-- =====================
CREATE TABLE bloqueos_agenda (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id     UUID        NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    fecha          DATE        NOT NULL,
    hora_inicio    TIME        NOT NULL,
    hora_fin       TIME        NOT NULL,
    motivo         TEXT,
    creado_por_id  UUID        REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_bloqueo_horas CHECK (hora_inicio < hora_fin)
);

-- =====================
-- TABLA: notificaciones
-- =====================
CREATE TABLE notificaciones (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo        VARCHAR(60) NOT NULL,
    titulo      VARCHAR(200) NOT NULL,
    cuerpo      TEXT        NOT NULL,
    leida       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================
-- TABLA: notif_config
-- =====================
CREATE TABLE notif_config (
    empresa_id             UUID         PRIMARY KEY REFERENCES empresas(id) ON DELETE CASCADE,
    whatsapp_api_token     TEXT,
    whatsapp_phone_id      VARCHAR(50),
    gmail_client_id        VARCHAR(200),
    gmail_client_secret    TEXT,
    whatsapp_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    email_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =====================
-- ÍNDICES
-- =====================
CREATE INDEX idx_usuarios_email           ON usuarios(email);
CREATE INDEX idx_usuarios_empresa         ON usuarios(empresa_id);
CREATE INDEX idx_turnos_empresa           ON turnos(empresa_id);
CREATE INDEX idx_turnos_cliente           ON turnos(cliente_id);
CREATE INDEX idx_turnos_estado            ON turnos(estado);
CREATE INDEX idx_turnos_empresa_fecha     ON turnos(empresa_id, fecha_confirmada);
CREATE INDEX idx_bloqueos_empresa_fecha   ON bloqueos_agenda(empresa_id, fecha);
CREATE INDEX idx_notificaciones_usuario   ON notificaciones(usuario_id);
CREATE INDEX idx_notificaciones_leida     ON notificaciones(usuario_id, leida);
CREATE INDEX idx_refresh_tokens_usuario   ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_hash      ON refresh_tokens(token_hash);
CREATE INDEX idx_turno_historial_turno    ON turno_historial(turno_id);
