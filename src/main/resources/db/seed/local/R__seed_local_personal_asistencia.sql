-- Asistencia ficticia SOLO para el perfil local: seis meses completos hacia atrás más el mes en curso, para cada
-- empleado vigente y cada día del calendario, con las mismas reglas que aplica el consolidador del sistema
-- (ConsolidadorAsistencia): DESCANSO fuera del horario, FERIADO, JUSTIFICADA con un permiso aprobado, FALTA sin
-- marcaciones, tardanza solo si la llegada supera la tolerancia y minutos extra después de la hora de salida.
--
-- Se ejecuta después de R__seed_local_personal.sql (el nombre ordena los repetibles) y es idempotente: solo crea los
-- días que aún no tienen asistencia consolidada.
--
-- DECISIÓN DE SEMILLA: el mes en curso se deja consolidado completo (también los días que aún no ocurrieron) para que
-- su planilla se pueda calcular y cerrar de inmediato en una demostración local. En una instalación real el último
-- día del mes se consolida recién cuando termina; aquí las marcaciones futuras son datos de prueba.

-- Los días de permisos aprobados no cuentan como falta.
INSERT INTO dia_justificado (tenant_id, empleado_id, fecha, motivo, origen_id)
SELECT p.tenant_id, p.empleado_id, d::date, 'PERMISO', p.id
  FROM permiso p
 CROSS JOIN LATERAL generate_series(p.desde::timestamp, p.hasta::timestamp, interval '1 day') d
 WHERE p.estado = 'APROBADO'
ON CONFLICT (tenant_id, empleado_id, fecha) DO NOTHING;

CREATE TEMP TABLE _seed_dias ON COMMIT DROP AS
WITH rango AS (
    SELECT (date_trunc('month', current_date) - interval '6 months')::date                              AS desde,
           (date_trunc('month', current_date) + interval '1 month' - interval '1 day')::date           AS hasta
), base AS (
    SELECT e.tenant_id, e.id AS empleado_id, d::date AS dia,
           h.hora_entrada, h.hora_salida, h.tolerancia_min, h.dias,
           abs(hashtext(e.id::text || d::date::text)) % 100 AS n
      FROM empleado e
     CROSS JOIN rango r
     CROSS JOIN LATERAL generate_series(r.desde::timestamp, r.hasta::timestamp, interval '1 day') d
      JOIN empleado_horario eh ON eh.empleado_id = e.id
                              AND eh.desde <= d::date AND (eh.hasta IS NULL OR eh.hasta >= d::date)
      JOIN horario h ON h.id = eh.horario_id
     WHERE e.id::text LIKE 'dddddddd-0000-4000-8000-%'
       AND e.fecha_ingreso <= d::date
       AND (e.fecha_cese IS NULL OR e.fecha_cese >= d::date)
       AND NOT EXISTS (SELECT 1 FROM asistencia_diaria a WHERE a.empleado_id = e.id AND a.fecha = d::date)
)
SELECT b.tenant_id, b.empleado_id, b.dia, b.hora_entrada, b.hora_salida, b.tolerancia_min, b.n,
       (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])[extract(isodow FROM b.dia)::int]
           = ANY (string_to_array(b.dias, ','))                                                  AS laborable,
       EXISTS (SELECT 1 FROM feriado f WHERE f.fecha = b.dia)                                    AS feriado,
       EXISTS (SELECT 1 FROM dia_justificado j
                WHERE j.empleado_id = b.empleado_id AND j.fecha = b.dia)                         AS justificado
  FROM base b;

ALTER TABLE _seed_dias ADD COLUMN estado varchar(20), ADD COLUMN atraso int NOT NULL DEFAULT 0,
                       ADD COLUMN extra int NOT NULL DEFAULT 0;

-- Estado del día en el mismo orden de prioridad que el consolidador.
UPDATE _seed_dias SET estado = CASE
    WHEN NOT laborable THEN 'DESCANSO'
    WHEN feriado       THEN 'FERIADO'
    WHEN justificado   THEN 'JUSTIFICADA'
    WHEN n < 2         THEN 'FALTA'
    ELSE 'ASISTIO' END;

-- Quien asiste llega entre 0 y 5 min tarde (la mayoría), hasta 10 (dentro de la tolerancia) o 11-35 min (tardanza),
-- y algunos días se queda 30-75 min después de la salida.
UPDATE _seed_dias SET
    atraso = CASE WHEN n < 14 THEN 11 + (n * 7) % 25
                  WHEN n < 40 THEN n % 11
                  ELSE n % 6 END,
    extra  = CASE WHEN n BETWEEN 45 AND 58 THEN 30 + ((n - 45) % 4) * 15 ELSE 0 END
 WHERE estado = 'ASISTIO';

INSERT INTO marcacion (tenant_id, empleado_id, instante, tipo, origen)
SELECT tenant_id, empleado_id,
       (dia + hora_entrada + atraso * interval '1 minute') AT TIME ZONE 'America/Lima', 'ENTRADA', 'WEB'
  FROM _seed_dias WHERE estado = 'ASISTIO'
UNION ALL
SELECT tenant_id, empleado_id,
       (dia + hora_salida + extra * interval '1 minute') AT TIME ZONE 'America/Lima', 'SALIDA', 'WEB'
  FROM _seed_dias WHERE estado = 'ASISTIO';

INSERT INTO asistencia_diaria (tenant_id, empleado_id, fecha, minutos_tardanza, minutos_extra, estado)
SELECT tenant_id, empleado_id, dia,
       CASE WHEN estado = 'ASISTIO' AND atraso > tolerancia_min THEN atraso ELSE 0 END,
       CASE WHEN estado = 'ASISTIO' THEN extra ELSE 0 END,
       CASE WHEN estado = 'ASISTIO' THEN (CASE WHEN atraso > tolerancia_min THEN 'TARDANZA' ELSE 'PRESENTE' END)
            ELSE estado END
  FROM _seed_dias
ON CONFLICT (tenant_id, empleado_id, fecha) DO NOTHING;

-- Horas extra: dos de cada tres días con minutos extra ya ocurridos tienen su autorización aprobada por RRHH.
INSERT INTO hora_extra (tenant_id, empleado_id, fecha, horas, motivo, estado, aprobado_por)
SELECT s.tenant_id, s.empleado_id, s.dia, round(s.extra / 60.0, 2), 'Atención de pedidos de la campaña',
       'APROBADA', (SELECT u.id FROM usuario u WHERE u.tenant_id = s.tenant_id AND u.rol = 'RRHH' LIMIT 1)
  FROM _seed_dias s
 WHERE s.estado = 'ASISTIO' AND s.extra >= 30 AND s.dia < current_date AND s.n % 3 <> 0
   AND NOT EXISTS (SELECT 1 FROM hora_extra x WHERE x.empleado_id = s.empleado_id AND x.fecha = s.dia);

-- Una solicitud pendiente de ejemplo para la bandeja de aprobaciones.
INSERT INTO hora_extra (id, tenant_id, empleado_id, fecha, horas, motivo, estado, aprobado_por) VALUES
    ('0e0e0e0e-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111',
     'dddddddd-0000-4000-8000-000000000003', current_date - 1, 2.00, 'Cierre de lote de exportación', 'PENDIENTE', NULL)
ON CONFLICT (id) DO NOTHING;
