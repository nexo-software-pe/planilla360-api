-- V8: boletas de pago (archivo PDF por trabajador y planilla) y la vista de lectura que usan los reportes.

CREATE TABLE boleta (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid         NOT NULL REFERENCES empresa (id),
    planilla_id   uuid         NOT NULL REFERENCES planilla (id),
    empleado_id   uuid         NOT NULL REFERENCES empleado (id),
    periodo       varchar(7)   NOT NULL,
    tipo          varchar(15)  NOT NULL,
    estado        varchar(15)  NOT NULL DEFAULT 'PENDIENTE',
    archivo_clave varchar(300),
    hash          varchar(64),
    tamano_bytes  integer,
    intentos      integer      NOT NULL DEFAULT 0,
    ultimo_error  varchar(300),
    generada_en   timestamptz,
    enviada_en    timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    created_by    uuid,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    uuid,
    CONSTRAINT uq_boleta_planilla_empleado UNIQUE (planilla_id, empleado_id),
    CONSTRAINT ck_boleta_estado CHECK (estado IN ('PENDIENTE', 'GENERADA', 'ENVIADA', 'FALLIDA')),
    CONSTRAINT ck_boleta_archivo CHECK (estado IN ('PENDIENTE', 'FALLIDA') OR archivo_clave IS NOT NULL)
);
CREATE INDEX ix_boleta_tenant_empleado ON boleta (tenant_id, empleado_id);
CREATE INDEX ix_boleta_planilla_estado ON boleta (planilla_id, estado);

-- Vista de solo lectura para los reportes (Shared Database en lectura, ver ADR del diseño): una fila por
-- trabajador de cada planilla cerrada, con su área y cargo. Las consultas nativas deben filtrar por tenant_id.
CREATE VIEW vista_planilla_detalle AS
SELECT p.tenant_id,
       p.id             AS planilla_id,
       p.periodo,
       p.tipo,
       e.id             AS empleado_id,
       e.dni,
       e.apellidos || ', ' || e.nombres AS trabajador,
       a.id             AS area_id,
       a.nombre         AS area,
       c.nombre         AS cargo,
       l.basico,
       l.asig_familiar,
       l.horas_extra,
       l.bonos,
       l.gratificacion,
       l.bonif_extra,
       l.cts,
       l.total_ingresos,
       l.total_descuentos,
       l.neto,
       l.aporte_essalud
  FROM planilla p
  JOIN linea_planilla l ON l.planilla_id = p.id
  JOIN empleado e       ON e.id = l.empleado_id
  JOIN area a           ON a.id = e.area_id
  JOIN cargo c          ON c.id = e.cargo_id
 WHERE p.estado = 'CERRADA';
