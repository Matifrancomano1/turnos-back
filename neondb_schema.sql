-- ============================================================
--  SaaS Gestión de Turnos — Schema completo para NeonDB
--  Versión: 2.0  (V1 + V2 consolidados)
--
--  Instrucciones:
--    1. Abre el SQL Editor en tu proyecto de NeonDB.
--    2. Pega TODO el contenido de este archivo.
--    3. Ejecuta con "Run" (o Ctrl+Enter).
--    4. El script es idempotente: usa IF NOT EXISTS y
--       DROP ... IF EXISTS para poder re-ejecutarse sin errores.
-- ============================================================


-- ============================================================
-- 0. LIMPIEZA (segura para re-ejecución en entorno de dev)
-- ============================================================
DROP TABLE IF EXISTS notif_config        CASCADE;
DROP TABLE IF EXISTS notificaciones      CASCADE;
DROP TABLE IF EXISTS bloqueos_agenda     CASCADE;
DROP TABLE IF EXISTS senias              CASCADE;
DROP TABLE IF EXISTS cotizaciones        CASCADE;
DROP TABLE IF EXISTS turno_historial     CASCADE;
DROP TABLE IF EXISTS turnos              CASCADE;
DROP TABLE IF EXISTS servicios           CASCADE;
DROP TABLE IF EXISTS refresh_tokens      CASCADE;
DROP TABLE IF EXISTS usuarios            CASCADE;
DROP TABLE IF EXISTS empresas            CASCADE;

DROP TYPE IF EXISTS cotizacion_estado;
DROP TYPE IF EXISTS turno_estado;
DROP TYPE IF EXISTS rol_enum;


-- ============================================================
-- 1. EXTENSIONES
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()


-- ============================================================
-- 2. TIPOS ENUM
-- ============================================================

CREATE TYPE rol_enum AS ENUM (
    'CLIENTE',
    'OPERADOR',
    'ADMIN'
);

CREATE TYPE turno_estado AS ENUM (
    'SOLICITADO',
    'EN_COTIZACION',
    'COTIZADO',
    'CONFIRMADO',
    'PROGRAMADO',
    'FINALIZADO',
    'CANCELADO'
);

CREATE TYPE cotizacion_estado AS ENUM (
    'PENDIENTE',
    'ACEPTADA',
    'RECHAZADA'
);


-- ============================================================
-- 3. FUNCIÓN TRIGGER: updated_at automático
-- ============================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- 4. TABLAS
-- ============================================================

-- ------------------------------------------------------------
-- 4.1 empresas
--   Cada fila es un tenant del SaaS.
--   El campo `slug` identifica la URL pública de la empresa.
--   Ejemplo: "mi-taller-mecanico" → /reservas/mi-taller-mecanico
-- ------------------------------------------------------------
CREATE TABLE empresas (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre                   VARCHAR(150) NOT NULL,
    slug                     VARCHAR(100) NOT NULL UNIQUE,        -- URL pública única
    email_contacto           VARCHAR(150) NOT NULL,
    direccion                VARCHAR(255),
    telefono                 VARCHAR(30),
    hora_apertura            TIME         NOT NULL DEFAULT '08:00',
    hora_cierre              TIME         NOT NULL DEFAULT '18:00',
    duracion_slot_minutos    INTEGER      NOT NULL DEFAULT 30
                                          CHECK (duracion_slot_minutos > 0
                                             AND duracion_slot_minutos % 15 = 0),  -- múltiplo de 15
    sabado_habilitado        BOOLEAN      NOT NULL DEFAULT FALSE,
    domingo_habilitado       BOOLEAN      NOT NULL DEFAULT FALSE,
    activa                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_horario CHECK (hora_cierre > hora_apertura)
);

CREATE TRIGGER trg_empresas_updated_at
    BEFORE UPDATE ON empresas
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ------------------------------------------------------------
-- 4.2 usuarios
--   Soporta los roles CLIENTE, OPERADOR y ADMIN.
--   Un ADMIN global tiene empresa_id = NULL.
-- ------------------------------------------------------------
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


-- ------------------------------------------------------------
-- 4.3 refresh_tokens
--   Almacena hashes de los tokens de refresco activos.
--   El token real NUNCA se guarda; solo su hash SHA-256.
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex del token
    tipo        VARCHAR(20) NOT NULL DEFAULT 'REFRESH',
    expira_en   TIMESTAMPTZ NOT NULL,
    revocado    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ------------------------------------------------------------
-- 4.4 servicios
--   Catálogo de servicios ofrecidos por cada empresa.
-- ------------------------------------------------------------
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


-- ------------------------------------------------------------
-- 4.5 turnos
--   Núcleo del sistema. Maneja el ciclo de vida completo
--   de un turno a través de la máquina de estados.
-- ------------------------------------------------------------
CREATE TABLE turnos (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID         NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    cliente_id        UUID         NOT NULL REFERENCES usuarios(id),
    servicio_id       UUID         REFERENCES servicios(id) ON DELETE SET NULL,
    fecha_solicitada  DATE,
    hora_solicitada   TIME,
    fecha_confirmada  DATE,
    hora_confirmada   TIME,
    estado            turno_estado NOT NULL DEFAULT 'SOLICITADO',
    observaciones     TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_turnos_updated_at
    BEFORE UPDATE ON turnos
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ------------------------------------------------------------
-- 4.6 turno_historial
--   Auditoría de cada cambio de estado de un turno.
-- ------------------------------------------------------------
CREATE TABLE turno_historial (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    turno_id         UUID         NOT NULL REFERENCES turnos(id) ON DELETE CASCADE,
    estado_anterior  turno_estado,
    estado_nuevo     turno_estado NOT NULL,
    motivo           TEXT,
    cambiado_por_id  UUID         REFERENCES usuarios(id) ON DELETE SET NULL,
    timestamp        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- ------------------------------------------------------------
-- 4.7 cotizaciones
--   Una cotización por turno (relación 1:1).
--   El operador cotiza el precio y duración real del trabajo.
-- ------------------------------------------------------------
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


-- ------------------------------------------------------------
-- 4.8 senias
--   Registro del pago de seña asociado a un turno (1:1).
-- ------------------------------------------------------------
CREATE TABLE senias (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    turno_id      UUID          NOT NULL UNIQUE REFERENCES turnos(id) ON DELETE CASCADE,
    monto         NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    metodo_pago   VARCHAR(50)   NOT NULL,
    referencia    VARCHAR(200),
    registrado_en TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);


-- ------------------------------------------------------------
-- 4.9 bloqueos_agenda
--   Períodos en los que la empresa no acepta turnos
--   (ej: feriados, mantenimiento, vacaciones).
-- ------------------------------------------------------------
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


-- ------------------------------------------------------------
-- 4.10 notificaciones
--   Bandeja de notificaciones in-app para cada usuario.
-- ------------------------------------------------------------
CREATE TABLE notificaciones (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo        VARCHAR(60)  NOT NULL,
    titulo      VARCHAR(200) NOT NULL,
    cuerpo      TEXT         NOT NULL,
    leida       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- ------------------------------------------------------------
-- 4.11 notif_config
--   Credenciales de notificación de cada empresa (WhatsApp / Gmail).
--   Clave primaria = empresa_id (relación 1:1 con empresas).
--   ⚠️  Los secretos (tokens, client_secret) deben estar
--       cifrados a nivel de aplicación antes de persistirse.
-- ------------------------------------------------------------
CREATE TABLE notif_config (
    empresa_id             UUID        PRIMARY KEY REFERENCES empresas(id) ON DELETE CASCADE,
    whatsapp_api_token     TEXT,                     -- cifrado en app
    whatsapp_phone_id      VARCHAR(50),
    gmail_client_id        VARCHAR(200),
    gmail_client_secret    TEXT,                     -- cifrado en app
    whatsapp_enabled       BOOLEAN     NOT NULL DEFAULT FALSE,
    email_enabled          BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 5. ÍNDICES DE RENDIMIENTO
-- ============================================================

-- Empresas
CREATE UNIQUE INDEX idx_empresas_slug            ON empresas(slug);

-- Usuarios
CREATE INDEX idx_usuarios_email                  ON usuarios(email);
CREATE INDEX idx_usuarios_empresa                ON usuarios(empresa_id);
CREATE INDEX idx_usuarios_rol                    ON usuarios(rol);

-- Turnos
CREATE INDEX idx_turnos_empresa                  ON turnos(empresa_id);
CREATE INDEX idx_turnos_cliente                  ON turnos(cliente_id);
CREATE INDEX idx_turnos_estado                   ON turnos(estado);
CREATE INDEX idx_turnos_empresa_fecha            ON turnos(empresa_id, fecha_confirmada);
CREATE INDEX idx_turnos_empresa_estado           ON turnos(empresa_id, estado);

-- Servicios
CREATE INDEX idx_servicios_empresa               ON servicios(empresa_id);
CREATE INDEX idx_servicios_empresa_activo        ON servicios(empresa_id, activo);

-- Agenda
CREATE INDEX idx_bloqueos_empresa_fecha          ON bloqueos_agenda(empresa_id, fecha);

-- Notificaciones
CREATE INDEX idx_notificaciones_usuario          ON notificaciones(usuario_id);
CREATE INDEX idx_notificaciones_no_leidas        ON notificaciones(usuario_id, leida) WHERE leida = FALSE;

-- Tokens
CREATE INDEX idx_refresh_tokens_usuario          ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_hash             ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_vigentes         ON refresh_tokens(usuario_id, revocado) WHERE revocado = FALSE;

-- Historial
CREATE INDEX idx_turno_historial_turno           ON turno_historial(turno_id);
CREATE INDEX idx_turno_historial_timestamp       ON turno_historial(turno_id, timestamp DESC);


-- ============================================================
-- 6. DATOS SEMILLA (opcional — mismos del DataSeeder)
--    Las contraseñas están hasheadas con BCrypt (cost=12).
--
--    ADMIN:    admin@turnos.com    / Admin1234!
--    OPERADOR: operador@turnos.com / Operador1234!
--
--    Genera nuevos hashes en: https://bcrypt-generator.com
-- ============================================================

INSERT INTO usuarios (id, nombre, email, password_hash, rol, activo)
VALUES
    (gen_random_uuid(),
     'Administrador Global',
     'admin@turnos.com',
     '$2a$12$1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',  -- reemplazar con hash real
     'ADMIN',
     TRUE),
    (gen_random_uuid(),
     'Operador Demo',
     'operador@turnos.com',
     '$2a$12$1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',  -- reemplazar con hash real
     'OPERADOR',
     TRUE)
ON CONFLICT (email) DO NOTHING;


-- ============================================================
-- FIN DEL SCHEMA
-- ============================================================
