-- V4: asistencia (horarios, marcaciones, consolidación diaria, horas extra, días justificados) y ShedLock.

CREATE TABLE horario (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      uuid        NOT NULL REFERENCES empresa (id),
    nombre         varchar(80) NOT NULL,
    hora_entrada   time        NOT NULL,
    hora_salida    time        NOT NULL,
    tolerancia_min integer     NOT NULL DEFAULT 10,
    dias           varchar(40) NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    created_at     timestamptz NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_at     timestamptz NOT NULL DEFAULT now(),
    updated_by     uuid,
    CONSTRAINT uq_horario_nombre UNIQUE (tenant_id, nombre),
    CONSTRAINT ck_horario_horas CHECK (hora_salida > hora_entrada),
    CONSTRAINT ck_horario_tolerancia CHECK (tolerancia_min BETWEEN 0 AND 60)
);

CREATE TABLE empleado_horario (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid        NOT NULL REFERENCES empresa (id),
    empleado_id uuid        NOT NULL REFERENCES empleado (id),
    horario_id  uuid        NOT NULL REFERENCES horario (id),
    desde       date        NOT NULL,
    hasta       date,
    created_at  timestamptz NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz NOT NULL DEFAULT now(),
    updated_by  uuid,
    CONSTRAINT ck_empleado_horario_fechas CHECK (hasta IS NULL OR hasta >= desde)
);
CREATE INDEX ix_empleado_horario ON empleado_horario (tenant_id, empleado_id, desde DESC);

CREATE TABLE marcacion (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid        NOT NULL REFERENCES empresa (id),
    empleado_id uuid        NOT NULL REFERENCES empleado (id),
    instante    timestamptz NOT NULL,
    tipo        varchar(10) NOT NULL,
    origen      varchar(20) NOT NULL DEFAULT 'WEB',
    created_at  timestamptz NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz NOT NULL DEFAULT now(),
    updated_by  uuid,
    CONSTRAINT ck_marcacion_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA'))
);
CREATE INDEX ix_marcacion_empleado_instante ON marcacion (tenant_id, empleado_id, instante);

CREATE TABLE asistencia_diaria (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        uuid        NOT NULL REFERENCES empresa (id),
    empleado_id      uuid        NOT NULL REFERENCES empleado (id),
    fecha            date        NOT NULL,
    minutos_tardanza integer     NOT NULL DEFAULT 0,
    minutos_extra    integer     NOT NULL DEFAULT 0,
    estado           varchar(20) NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    created_by       uuid,
    updated_at       timestamptz NOT NULL DEFAULT now(),
    updated_by       uuid,
    CONSTRAINT uq_asistencia_diaria UNIQUE (tenant_id, empleado_id, fecha),
    CONSTRAINT ck_asistencia_estado CHECK (estado IN
        ('PRESENTE', 'TARDANZA', 'FALTA', 'JUSTIFICADA', 'FERIADO', 'DESCANSO', 'INCOMPLETA')),
    CONSTRAINT ck_asistencia_minutos CHECK (minutos_tardanza >= 0 AND minutos_extra >= 0)
);
CREATE INDEX ix_asistencia_fecha ON asistencia_diaria (tenant_id, fecha);

CREATE TABLE hora_extra (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    uuid          NOT NULL REFERENCES empresa (id),
    empleado_id  uuid          NOT NULL REFERENCES empleado (id),
    fecha        date          NOT NULL,
    horas        numeric(4, 2) NOT NULL,
    motivo       varchar(200),
    estado       varchar(15)   NOT NULL DEFAULT 'PENDIENTE',
    aprobado_por uuid,
    created_at   timestamptz   NOT NULL DEFAULT now(),
    created_by   uuid,
    updated_at   timestamptz   NOT NULL DEFAULT now(),
    updated_by   uuid,
    CONSTRAINT ck_hora_extra_horas CHECK (horas > 0 AND horas <= 8),
    CONSTRAINT ck_hora_extra_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);
CREATE INDEX ix_hora_extra_empleado ON hora_extra (tenant_id, empleado_id, fecha);

-- Días sin marcación que no cuentan como falta (vacaciones, permisos). Los llena el módulo al
-- escuchar los eventos de vacaciones.
CREATE TABLE dia_justificado (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid        NOT NULL REFERENCES empresa (id),
    empleado_id uuid        NOT NULL REFERENCES empleado (id),
    fecha       date        NOT NULL,
    motivo      varchar(30) NOT NULL,
    origen_id   uuid        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz NOT NULL DEFAULT now(),
    updated_by  uuid,
    CONSTRAINT uq_dia_justificado UNIQUE (tenant_id, empleado_id, fecha),
    CONSTRAINT ck_dia_justificado_motivo CHECK (motivo IN ('VACACIONES', 'PERMISO'))
);
CREATE INDEX ix_dia_justificado_origen ON dia_justificado (tenant_id, origen_id);

-- Bloqueo distribuido de los jobs programados (ShedLock).
CREATE TABLE shedlock (
    name       varchar(64)  NOT NULL PRIMARY KEY,
    lock_until timestamptz  NOT NULL,
    locked_at  timestamptz  NOT NULL,
    locked_by  varchar(255) NOT NULL
);
