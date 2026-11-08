-- V5: vacaciones (periodos, solicitudes) y permisos.

CREATE TABLE periodo_vacacional (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid        NOT NULL REFERENCES empresa (id),
    empleado_id   uuid        NOT NULL REFERENCES empleado (id),
    desde         date        NOT NULL,
    hasta         date        NOT NULL,
    dias_ganados  integer     NOT NULL,
    dias_gozados  integer     NOT NULL DEFAULT 0,
    dias_vendidos integer     NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    created_by    uuid,
    updated_at    timestamptz NOT NULL DEFAULT now(),
    updated_by    uuid,
    CONSTRAINT uq_periodo_vacacional UNIQUE (tenant_id, empleado_id, desde),
    CONSTRAINT ck_periodo_fechas CHECK (hasta > desde),
    CONSTRAINT ck_periodo_dias CHECK (
        dias_ganados >= 0 AND dias_gozados >= 0 AND dias_vendidos >= 0
        AND dias_gozados + dias_vendidos <= dias_ganados)
);

CREATE TABLE solicitud_vacaciones (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      uuid         NOT NULL REFERENCES empresa (id),
    empleado_id    uuid         NOT NULL REFERENCES empleado (id),
    periodo_id     uuid         NOT NULL REFERENCES periodo_vacacional (id),
    desde          date         NOT NULL,
    hasta          date         NOT NULL,
    dias           integer      NOT NULL,
    estado         varchar(20)  NOT NULL DEFAULT 'SOLICITADA',
    motivo_rechazo varchar(300),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     uuid,
    CONSTRAINT ck_solicitud_fechas CHECK (hasta >= desde),
    CONSTRAINT ck_solicitud_dias CHECK (dias > 0),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN
        ('SOLICITADA', 'APROBADA_JEFE', 'APROBADA_RRHH', 'EN_GOCE', 'FINALIZADA', 'RECHAZADA', 'CANCELADA'))
);
CREATE INDEX ix_solicitud_empleado ON solicitud_vacaciones (tenant_id, empleado_id, desde);
CREATE INDEX ix_solicitud_estado ON solicitud_vacaciones (tenant_id, estado);

CREATE TABLE permiso (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      uuid         NOT NULL REFERENCES empresa (id),
    empleado_id    uuid         NOT NULL REFERENCES empleado (id),
    tipo           varchar(30)  NOT NULL,
    desde          date         NOT NULL,
    hasta          date         NOT NULL,
    con_goce       boolean      NOT NULL,
    motivo         varchar(300),
    estado         varchar(15)  NOT NULL DEFAULT 'SOLICITADO',
    motivo_rechazo varchar(300),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     uuid,
    CONSTRAINT ck_permiso_tipo CHECK (tipo IN ('MEDICO', 'PERSONAL', 'LICENCIA')),
    CONSTRAINT ck_permiso_fechas CHECK (hasta >= desde),
    CONSTRAINT ck_permiso_estado CHECK (estado IN ('SOLICITADO', 'APROBADO', 'RECHAZADO'))
);
CREATE INDEX ix_permiso_empleado ON permiso (tenant_id, empleado_id, desde);
