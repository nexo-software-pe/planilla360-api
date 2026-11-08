-- V3: empleados, contratos y familiares.

CREATE TABLE empleado (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        uuid         NOT NULL REFERENCES empresa (id),
    dni              varchar(8)   NOT NULL,
    nombres          varchar(100) NOT NULL,
    apellidos        varchar(100) NOT NULL,
    fecha_nacimiento date         NOT NULL,
    email            varchar(160),
    telefono         varchar(20),
    fecha_ingreso    date         NOT NULL,
    fecha_cese       date,
    area_id          uuid         NOT NULL REFERENCES area (id),
    cargo_id         uuid         NOT NULL REFERENCES cargo (id),
    jefe_id          uuid REFERENCES empleado (id),
    estado           varchar(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at       timestamptz  NOT NULL DEFAULT now(),
    created_by       uuid,
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    updated_by       uuid,
    CONSTRAINT uq_empleado_dni UNIQUE (tenant_id, dni),
    CONSTRAINT ck_empleado_estado CHECK (estado IN ('ACTIVO', 'VACACIONES', 'LICENCIA', 'CESADO')),
    CONSTRAINT ck_empleado_cese CHECK ((estado = 'CESADO') = (fecha_cese IS NOT NULL)),
    CONSTRAINT ck_empleado_jefe CHECK (jefe_id IS NULL OR jefe_id <> id)
);
CREATE INDEX ix_empleado_tenant_area_estado ON empleado (tenant_id, area_id, estado);
CREATE INDEX ix_empleado_tenant_apellidos ON empleado (tenant_id, lower(apellidos), lower(nombres));
CREATE INDEX ix_empleado_jefe ON empleado (tenant_id, jefe_id);

CREATE TABLE contrato (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           uuid           NOT NULL REFERENCES empresa (id),
    empleado_id         uuid           NOT NULL REFERENCES empleado (id),
    tipo                varchar(30)    NOT NULL,
    fecha_inicio        date           NOT NULL,
    fecha_fin           date,
    sueldo_basico       numeric(12, 2) NOT NULL,
    jornada_semanal     integer        NOT NULL DEFAULT 48,
    regimen_pensionario varchar(10)    NOT NULL,
    afp_id              uuid REFERENCES afp (id),
    tipo_comision       varchar(10),
    cuspp               varchar(12),
    created_at          timestamptz    NOT NULL DEFAULT now(),
    created_by          uuid,
    updated_at          timestamptz    NOT NULL DEFAULT now(),
    updated_by          uuid,
    CONSTRAINT ck_contrato_tipo CHECK (tipo IN ('INDEFINIDO', 'PLAZO_FIJO', 'PART_TIME')),
    CONSTRAINT ck_contrato_sueldo CHECK (sueldo_basico > 0),
    CONSTRAINT ck_contrato_jornada CHECK (jornada_semanal BETWEEN 1 AND 48),
    CONSTRAINT ck_contrato_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT ck_contrato_regimen CHECK (regimen_pensionario IN ('ONP', 'AFP')),
    CONSTRAINT ck_contrato_comision CHECK (tipo_comision IS NULL OR tipo_comision IN ('FLUJO', 'MIXTA')),
    CONSTRAINT ck_contrato_afp CHECK (
        (regimen_pensionario = 'AFP' AND afp_id IS NOT NULL AND tipo_comision IS NOT NULL)
        OR (regimen_pensionario = 'ONP' AND afp_id IS NULL AND tipo_comision IS NULL))
);
CREATE INDEX ix_contrato_tenant_empleado ON contrato (tenant_id, empleado_id, fecha_inicio DESC);

CREATE TABLE familiar (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        uuid         NOT NULL REFERENCES empresa (id),
    empleado_id      uuid         NOT NULL REFERENCES empleado (id),
    parentesco       varchar(20)  NOT NULL,
    nombres          varchar(150) NOT NULL,
    fecha_nacimiento date         NOT NULL,
    estudia          boolean      NOT NULL DEFAULT false,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    created_by       uuid,
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    updated_by       uuid,
    CONSTRAINT ck_familiar_parentesco CHECK (parentesco IN ('HIJO', 'CONYUGE', 'CONVIVIENTE', 'PADRE', 'MADRE'))
);
CREATE INDEX ix_familiar_tenant_empleado ON familiar (tenant_id, empleado_id);

-- El vínculo usuario -> empleado ya existía como columna; ahora tiene destino.
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_empleado FOREIGN KEY (empleado_id) REFERENCES empleado (id);
