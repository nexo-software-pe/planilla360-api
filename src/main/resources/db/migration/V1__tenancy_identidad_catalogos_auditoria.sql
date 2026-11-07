-- V1: tenancy, identidad, catálogos y auditoría.
-- Convenciones: PK uuid, dinero numeric(12,2), tenant_id NOT NULL en tablas de negocio,
-- columnas de auditoría (created_at, created_by, updated_at, updated_by).

CREATE TABLE empresa (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ruc              varchar(11)  NOT NULL,
    razon_social     varchar(200) NOT NULL,
    regimen_laboral  varchar(30)  NOT NULL,
    plan             varchar(30)  NOT NULL,
    activa           boolean      NOT NULL DEFAULT true,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    created_by       uuid,
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    updated_by       uuid,
    CONSTRAINT uq_empresa_ruc UNIQUE (ruc),
    CONSTRAINT ck_empresa_regimen CHECK (regimen_laboral IN ('GENERAL', 'PEQUENA_EMPRESA')),
    CONSTRAINT ck_empresa_plan CHECK (plan IN ('BASICO', 'PROFESIONAL', 'EMPRESARIAL'))
);

CREATE TABLE usuario (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         uuid REFERENCES empresa (id),
    email             varchar(160) NOT NULL,
    nombre            varchar(150) NOT NULL,
    password_hash     varchar(100) NOT NULL,
    rol               varchar(30)  NOT NULL,
    empleado_id       uuid,
    activo            boolean      NOT NULL DEFAULT true,
    intentos_fallidos integer      NOT NULL DEFAULT 0,
    bloqueado_hasta   timestamptz,
    ultimo_acceso     timestamptz,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    created_by        uuid,
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    updated_by        uuid,
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN_SAAS', 'ADMIN_EMPRESA', 'RRHH', 'EMPLEADO')),
    -- tenant_id nulo solo para el administrador de la plataforma
    CONSTRAINT ck_usuario_tenant CHECK ((rol = 'ADMIN_SAAS') = (tenant_id IS NULL))
);
CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(email));
CREATE INDEX ix_usuario_tenant_rol ON usuario (tenant_id, rol);

CREATE TABLE refresh_token (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  uuid        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash  varchar(64) NOT NULL,
    expira_en   timestamptz NOT NULL,
    revocado    boolean     NOT NULL DEFAULT false,
    creado_en   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);

CREATE TABLE area (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid         NOT NULL REFERENCES empresa (id),
    nombre      varchar(100) NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid,
    CONSTRAINT uq_area_nombre UNIQUE (tenant_id, nombre)
);

CREATE TABLE cargo (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          uuid          NOT NULL REFERENCES empresa (id),
    area_id            uuid          NOT NULL REFERENCES area (id),
    nombre             varchar(100)  NOT NULL,
    sueldo_referencial numeric(12, 2) NOT NULL DEFAULT 0,
    created_at         timestamptz   NOT NULL DEFAULT now(),
    created_by         uuid,
    updated_at         timestamptz   NOT NULL DEFAULT now(),
    updated_by         uuid,
    CONSTRAINT uq_cargo_nombre UNIQUE (tenant_id, area_id, nombre),
    CONSTRAINT ck_cargo_sueldo CHECK (sueldo_referencial >= 0)
);
CREATE INDEX ix_cargo_tenant_area ON cargo (tenant_id, area_id);

-- Catálogos globales (sin tenant): los administra la plataforma.
CREATE TABLE afp (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         varchar(60)   NOT NULL,
    comision_flujo numeric(5, 2) NOT NULL,
    comision_mixta numeric(5, 2) NOT NULL,
    vigente_desde  date          NOT NULL,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    updated_by     uuid,
    CONSTRAINT uq_afp_nombre UNIQUE (nombre, vigente_desde)
);

CREATE TABLE parametro_legal (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo         varchar(60)    NOT NULL,
    descripcion    varchar(200)   NOT NULL,
    valor          numeric(14, 4) NOT NULL,
    vigente_desde  date           NOT NULL,
    vigente_hasta  date,
    created_at     timestamptz    NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_at     timestamptz    NOT NULL DEFAULT now(),
    updated_by     uuid,
    CONSTRAINT uq_parametro_vigencia UNIQUE (codigo, vigente_desde),
    CONSTRAINT ck_parametro_vigencia CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde)
);

CREATE TABLE feriado (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha       date         NOT NULL,
    descripcion varchar(120) NOT NULL,
    ambito      varchar(20)  NOT NULL DEFAULT 'NACIONAL',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid,
    CONSTRAINT uq_feriado UNIQUE (fecha, ambito),
    CONSTRAINT ck_feriado_ambito CHECK (ambito IN ('NACIONAL', 'REGIONAL'))
);

-- Historial de cambios de entidades sensibles.
CREATE TABLE auditoria_cambio (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid,
    entidad     varchar(60)  NOT NULL,
    entidad_id  uuid         NOT NULL,
    accion      varchar(20)  NOT NULL,
    usuario     varchar(160) NOT NULL,
    instante    timestamptz  NOT NULL DEFAULT now(),
    antes       jsonb,
    despues     jsonb,
    CONSTRAINT ck_auditoria_accion CHECK (accion IN ('CREAR', 'ACTUALIZAR', 'ELIMINAR'))
);
CREATE INDEX ix_auditoria_tenant_instante ON auditoria_cambio (tenant_id, instante DESC);
CREATE INDEX ix_auditoria_entidad ON auditoria_cambio (tenant_id, entidad, entidad_id);
