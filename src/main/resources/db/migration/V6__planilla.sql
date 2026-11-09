-- V6: planillas mensuales, de gratificación y de CTS, con sus líneas (boletas) y el desglose de conceptos.

CREATE TABLE planilla (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       uuid         NOT NULL REFERENCES empresa (id),
    periodo         varchar(7)   NOT NULL,
    tipo            varchar(15)  NOT NULL,
    estado          varchar(15)  NOT NULL DEFAULT 'BORRADOR',
    idempotency_key varchar(100),
    cerrada_en      timestamptz,
    trabajadores    integer        NOT NULL DEFAULT 0,
    total_ingresos  numeric(14, 2) NOT NULL DEFAULT 0,
    total_descuentos numeric(14, 2) NOT NULL DEFAULT 0,
    total_neto       numeric(14, 2) NOT NULL DEFAULT 0,
    total_essalud    numeric(14, 2) NOT NULL DEFAULT 0,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    created_by      uuid,
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      uuid,
    CONSTRAINT ck_planilla_periodo CHECK (periodo ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ck_planilla_tipo CHECK (tipo IN ('MENSUAL', 'GRATIFICACION', 'CTS')),
    CONSTRAINT ck_planilla_estado CHECK (estado IN ('BORRADOR', 'CALCULADA', 'CERRADA', 'ANULADA')),
    CONSTRAINT ck_planilla_cierre CHECK (estado <> 'CERRADA' OR (idempotency_key IS NOT NULL AND cerrada_en IS NOT NULL))
);
-- Solo una planilla cerrada por empresa, periodo y tipo (la anulada deja de contar y permite rehacerla).
CREATE UNIQUE INDEX uq_planilla_cerrada ON planilla (tenant_id, periodo, tipo) WHERE estado = 'CERRADA';
-- La misma clave de idempotencia no puede identificar dos planillas de una empresa.
CREATE UNIQUE INDEX uq_planilla_clave ON planilla (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_planilla_tenant_periodo ON planilla (tenant_id, periodo DESC, tipo);

CREATE TABLE linea_planilla (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             uuid           NOT NULL REFERENCES empresa (id),
    planilla_id           uuid           NOT NULL REFERENCES planilla (id) ON DELETE CASCADE,
    empleado_id           uuid           NOT NULL REFERENCES empleado (id),
    orden                 integer        NOT NULL,
    basico                numeric(12, 2) NOT NULL,
    asig_familiar         numeric(12, 2) NOT NULL,
    horas_extra           numeric(12, 2) NOT NULL,
    bonos                 numeric(12, 2) NOT NULL,
    gratificacion         numeric(12, 2) NOT NULL,
    bonif_extra           numeric(12, 2) NOT NULL,
    cts                   numeric(12, 2) NOT NULL,
    regimen_pensionario   varchar(10)    NOT NULL,
    afp_nombre            varchar(60),
    afp_comision_pct      numeric(7, 4),
    pension_aporte        numeric(12, 2) NOT NULL,
    pension_prima         numeric(12, 2) NOT NULL,
    pension_comision      numeric(12, 2) NOT NULL,
    retencion_5ta         numeric(12, 2) NOT NULL,
    descuento_asistencia  numeric(12, 2) NOT NULL,
    otros_descuentos      numeric(12, 2) NOT NULL,
    aporte_essalud        numeric(12, 2) NOT NULL,
    total_ingresos        numeric(12, 2) NOT NULL,
    total_descuentos      numeric(12, 2) NOT NULL,
    neto                  numeric(12, 2) NOT NULL,
    created_at            timestamptz    NOT NULL DEFAULT now(),
    created_by            uuid,
    updated_at            timestamptz    NOT NULL DEFAULT now(),
    updated_by            uuid,
    CONSTRAINT uq_linea_planilla_empleado UNIQUE (planilla_id, empleado_id),
    CONSTRAINT ck_linea_regimen CHECK (regimen_pensionario IN ('ONP', 'AFP')),
    CONSTRAINT ck_linea_afp CHECK (
        (regimen_pensionario = 'AFP' AND afp_nombre IS NOT NULL AND afp_comision_pct IS NOT NULL)
        OR (regimen_pensionario = 'ONP' AND afp_nombre IS NULL AND afp_comision_pct IS NULL))
);
CREATE INDEX ix_linea_planilla_planilla ON linea_planilla (planilla_id);
CREATE INDEX ix_linea_planilla_tenant_empleado ON linea_planilla (tenant_id, empleado_id);

-- Desglose de la boleta: un renglón por concepto con monto, en orden de impresión.
CREATE TABLE concepto_planilla (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid           NOT NULL REFERENCES empresa (id),
    linea_id    uuid           NOT NULL REFERENCES linea_planilla (id) ON DELETE CASCADE,
    orden       integer        NOT NULL,
    codigo      varchar(40)    NOT NULL,
    descripcion varchar(120)   NOT NULL,
    tipo        varchar(20)    NOT NULL,
    monto       numeric(12, 2) NOT NULL,
    CONSTRAINT ck_concepto_tipo CHECK (tipo IN ('INGRESO', 'DESCUENTO', 'APORTE_EMPLEADOR'))
);
CREATE INDEX ix_concepto_planilla_linea ON concepto_planilla (linea_id, orden);

-- Pago de una planilla cerrada. Mientras exista, la planilla no se puede anular.
CREATE TABLE pago_planilla (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   uuid         NOT NULL REFERENCES empresa (id),
    planilla_id uuid         NOT NULL REFERENCES planilla (id),
    fecha_pago  date         NOT NULL,
    referencia  varchar(80)  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid,
    CONSTRAINT uq_pago_planilla UNIQUE (planilla_id)
);
