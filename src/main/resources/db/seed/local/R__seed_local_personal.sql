-- Datos ficticios de personal, asistencia y vacaciones SOLO para el perfil local. Se ejecuta después de
-- R__seed_local.sql (que crea las empresas, usuarios, áreas y cargos). Es repetible e idempotente: las filas
-- con identificador fijo usan ON CONFLICT y la asistencia generada solo se crea si aún no existe.

-- ---------------------------------------------------------------------------------------------
-- Empleados: Textiles Andinos S.A.C. (…1111) y Panadería La Espiga E.I.R.L. (…2222)
-- ---------------------------------------------------------------------------------------------
INSERT INTO empleado (id, tenant_id, dni, nombres, apellidos, fecha_nacimiento, email, telefono, fecha_ingreso,
                      fecha_cese, area_id, cargo_id, jefe_id, estado) VALUES
    ('dddddddd-0000-4000-8000-00000000000a', '11111111-1111-4111-8111-111111111111', '40125010', 'Víctor',   'Paredes León',    DATE '1984-03-12', 'vparedes@textilesandinos.ejemplo.pe', '987000010', DATE '2019-04-01', NULL, 'bbbbbbbb-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000004', NULL, 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', '40125002', 'Jorge',    'Castillo Vega',   DATE '1982-07-21', 'jcastillo@textilesandinos.ejemplo.pe', '987000002', DATE '2018-09-03', NULL, 'bbbbbbbb-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000002', NULL, 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000007', '11111111-1111-4111-8111-111111111111', '40125007', 'Patricia', 'Rojas Díaz',      DATE '1990-11-02', 'projas@textilesandinos.ejemplo.pe',    '987000007', DATE '2021-02-15', NULL, 'bbbbbbbb-0000-4000-8000-000000000002', 'cccccccc-0000-4000-8000-000000000003', NULL, 'ACTIVO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO empleado (id, tenant_id, dni, nombres, apellidos, fecha_nacimiento, email, telefono, fecha_ingreso,
                      fecha_cese, area_id, cargo_id, jefe_id, estado) VALUES
    ('dddddddd-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', '40125001', 'Lucía',    'Flores Paredes',  DATE '1995-05-14', 'empleado@textilesandinos.ejemplo.pe',  '987000001', DATE '2023-02-01', NULL, 'bbbbbbbb-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000004', 'dddddddd-0000-4000-8000-00000000000a', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', '40125003', 'Mateo',    'Quispe Huamán',   DATE '1998-01-30', 'mquispe@textilesandinos.ejemplo.pe',   '987000003', DATE '2024-03-01', NULL, 'bbbbbbbb-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000001', 'dddddddd-0000-4000-8000-000000000002', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', '40125004', 'Rosa',     'Chávez Luna',     DATE '1993-09-09', 'rchavez@textilesandinos.ejemplo.pe',   '987000004', DATE '2022-06-01', NULL, 'bbbbbbbb-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000001', 'dddddddd-0000-4000-8000-000000000002', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000005', '11111111-1111-4111-8111-111111111111', '40125005', 'Pedro',    'Ramos Soto',      DATE '1989-12-25', 'pramos@textilesandinos.ejemplo.pe',    '987000005', DATE '2020-10-12', NULL, 'bbbbbbbb-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000001', 'dddddddd-0000-4000-8000-000000000002', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000006', '11111111-1111-4111-8111-111111111111', '40125006', 'Elena',    'Vargas Cruz',     DATE '1996-04-18', 'evargas@textilesandinos.ejemplo.pe',   '987000006', DATE '2025-05-05', NULL, 'bbbbbbbb-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000001', 'dddddddd-0000-4000-8000-000000000002', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000008', '11111111-1111-4111-8111-111111111111', '40125008', 'Miguel',   'Torres Ñaupa',    DATE '1992-08-08', 'mtorres@textilesandinos.ejemplo.pe',   '987000008', DATE '2021-08-23', NULL, 'bbbbbbbb-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000004', 'dddddddd-0000-4000-8000-00000000000a', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000009', '11111111-1111-4111-8111-111111111111', '40125009', 'Ana',      'Salazar Mego',    DATE '1994-02-27', 'asalazar@textilesandinos.ejemplo.pe',  '987000009', DATE '2023-09-18', NULL, 'bbbbbbbb-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000004', 'dddddddd-0000-4000-8000-00000000000a', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-00000000000b', '11111111-1111-4111-8111-111111111111', '40125011', 'Raúl',     'Medina Pozo',     DATE '1987-06-16', 'rmedina@textilesandinos.ejemplo.pe',   '987000011', DATE '2019-01-07', DATE '2026-06-30', 'bbbbbbbb-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000001', 'dddddddd-0000-4000-8000-000000000002', 'CESADO'),
    ('dddddddd-0000-4000-8000-000000000011', '22222222-2222-4222-8222-222222222222', '41230011', 'Carmen',   'Gutiérrez Ponce', DATE '1985-10-05', 'cgutierrez@laespiga.ejemplo.pe',       '986000011', DATE '2017-03-06', NULL, 'bbbbbbbb-0000-4000-8000-000000000004', 'cccccccc-0000-4000-8000-000000000005', NULL, 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000012', '22222222-2222-4222-8222-222222222222', '41230012', 'Luis',     'Mamani Quispe',   DATE '1991-01-19', 'lmamani@laespiga.ejemplo.pe',          '986000012', DATE '2022-11-14', NULL, 'bbbbbbbb-0000-4000-8000-000000000004', 'cccccccc-0000-4000-8000-000000000005', 'dddddddd-0000-4000-8000-000000000011', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000013', '22222222-2222-4222-8222-222222222222', '41230013', 'Sofía',    'Aguilar Díaz',    DATE '1999-07-07', 'saguilar@laespiga.ejemplo.pe',         '986000013', DATE '2024-06-03', NULL, 'bbbbbbbb-0000-4000-8000-000000000005', 'cccccccc-0000-4000-8000-000000000006', 'dddddddd-0000-4000-8000-000000000011', 'ACTIVO'),
    ('dddddddd-0000-4000-8000-000000000014', '22222222-2222-4222-8222-222222222222', '41230014', 'Diego',    'Herrera Ríos',    DATE '2000-03-23', 'dherrera@laespiga.ejemplo.pe',         '986000014', DATE '2025-09-01', NULL, 'bbbbbbbb-0000-4000-8000-000000000005', 'cccccccc-0000-4000-8000-000000000006', 'dddddddd-0000-4000-8000-000000000011', 'ACTIVO')
ON CONFLICT (id) DO NOTHING;

-- La cuenta de empleado de Textiles Andinos queda vinculada a Lucía Flores.
UPDATE usuario SET empleado_id = 'dddddddd-0000-4000-8000-000000000001'
 WHERE id = 'aaaaaaaa-0000-4000-8000-000000000004' AND empleado_id IS DISTINCT FROM 'dddddddd-0000-4000-8000-000000000001';

-- Contratos (ONP o AFP con comisión flujo/mixta).
INSERT INTO contrato (id, tenant_id, empleado_id, tipo, fecha_inicio, fecha_fin, sueldo_basico, jornada_semanal,
                      regimen_pensionario, afp_id, tipo_comision, cuspp) VALUES
    ('c0c0c0c0-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', 'INDEFINIDO',  DATE '2023-02-01', NULL, 2200.00, 48, 'AFP', (SELECT id FROM afp WHERE nombre = 'Prima'   LIMIT 1), 'FLUJO', '601234ABCDE1'),
    ('c0c0c0c0-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000002', 'INDEFINIDO',  DATE '2018-09-03', NULL, 3200.00, 48, 'AFP', (SELECT id FROM afp WHERE nombre = 'Integra' LIMIT 1), 'MIXTA', '602345BCDEF2'),
    ('c0c0c0c0-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000003', 'PLAZO_FIJO',  DATE '2024-03-01', DATE '2027-02-28', 1300.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000004', 'INDEFINIDO',  DATE '2022-06-01', NULL, 1450.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000005', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000005', 'INDEFINIDO',  DATE '2020-10-12', NULL, 1500.00, 48, 'AFP', (SELECT id FROM afp WHERE nombre = 'Profuturo' LIMIT 1), 'FLUJO', '603456CDEFG3'),
    ('c0c0c0c0-0000-4000-8000-000000000006', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000006', 'PLAZO_FIJO',  DATE '2025-05-05', DATE '2026-12-31', 1300.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000007', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000007', 'INDEFINIDO',  DATE '2021-02-15', NULL, 2800.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000008', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000008', 'INDEFINIDO',  DATE '2021-08-23', NULL, 2300.00, 48, 'AFP', (SELECT id FROM afp WHERE nombre = 'Prima'   LIMIT 1), 'MIXTA', '604567DEFGH4'),
    ('c0c0c0c0-0000-4000-8000-000000000009', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000009', 'INDEFINIDO',  DATE '2023-09-18', NULL, 2200.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-00000000000a', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-00000000000a', 'INDEFINIDO',  DATE '2019-04-01', NULL, 4500.00, 48, 'AFP', (SELECT id FROM afp WHERE nombre = 'Integra' LIMIT 1), 'FLUJO', '605678EFGHI5'),
    ('c0c0c0c0-0000-4000-8000-00000000000b', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-00000000000b', 'INDEFINIDO',  DATE '2019-01-07', DATE '2026-06-30', 1400.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000011', '22222222-2222-4222-8222-222222222222', 'dddddddd-0000-4000-8000-000000000011', 'INDEFINIDO',  DATE '2017-03-06', NULL, 1900.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000012', '22222222-2222-4222-8222-222222222222', 'dddddddd-0000-4000-8000-000000000012', 'INDEFINIDO',  DATE '2022-11-14', NULL, 1500.00, 48, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000013', '22222222-2222-4222-8222-222222222222', 'dddddddd-0000-4000-8000-000000000013', 'PART_TIME',   DATE '2024-06-03', NULL, 1130.00, 24, 'ONP', NULL, NULL, NULL),
    ('c0c0c0c0-0000-4000-8000-000000000014', '22222222-2222-4222-8222-222222222222', 'dddddddd-0000-4000-8000-000000000014', 'PLAZO_FIJO',  DATE '2025-09-01', DATE '2026-12-31', 1200.00, 48, 'ONP', NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO familiar (id, tenant_id, empleado_id, parentesco, nombres, fecha_nacimiento, estudia) VALUES
    ('fafafafa-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000002', 'HIJO',     'Camila Castillo Soto',  DATE '2012-04-02', true),
    ('fafafafa-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000002', 'CONYUGE',  'Sandra Soto Rivas',     DATE '1984-12-11', false),
    ('fafafafa-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000004', 'HIJO',     'Joaquín Torres Chávez', DATE '2020-09-15', false),
    ('fafafafa-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000005', 'HIJO',     'Valeria Ramos Pérez',   DATE '2006-02-20', true),
    ('fafafafa-0000-4000-8000-000000000005', '22222222-2222-4222-8222-222222222222', 'dddddddd-0000-4000-8000-000000000011', 'HIJO',     'Mía Gutiérrez Ponce',   DATE '2015-06-30', true)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------------------------
-- Horarios y asignaciones
-- ---------------------------------------------------------------------------------------------
INSERT INTO horario (id, tenant_id, nombre, hora_entrada, hora_salida, tolerancia_min, refrigerio_min, dias) VALUES
    ('eeeeeeee-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'Oficina',       TIME '08:00', TIME '17:00', 10, 60, 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY'),
    ('eeeeeeee-0000-4000-8000-000000000002', '22222222-2222-4222-8222-222222222222', 'Turno mañana',  TIME '06:00', TIME '14:00', 10, 0, 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY'),
    ('eeeeeeee-0000-4000-8000-000000000003', '22222222-2222-4222-8222-222222222222', 'Medio tiempo',   TIME '06:00', TIME '14:00', 10, 0, 'MONDAY,TUESDAY,WEDNESDAY')
ON CONFLICT (id) DO UPDATE SET refrigerio_min = EXCLUDED.refrigerio_min;

INSERT INTO empleado_horario (tenant_id, empleado_id, horario_id, desde, hasta)
SELECT e.tenant_id, e.id,
       CASE WHEN e.tenant_id = '11111111-1111-4111-8111-111111111111'
            THEN 'eeeeeeee-0000-4000-8000-000000000001'::uuid
            WHEN e.id = 'dddddddd-0000-4000-8000-000000000013' -- jornada de 24 h semanales (contrato part-time)
            THEN 'eeeeeeee-0000-4000-8000-000000000003'::uuid
            ELSE 'eeeeeeee-0000-4000-8000-000000000002'::uuid END,
       e.fecha_ingreso, e.fecha_cese
  FROM empleado e
 WHERE e.id::text LIKE 'dddddddd-0000-4000-8000-%'
   AND NOT EXISTS (SELECT 1 FROM empleado_horario eh WHERE eh.empleado_id = e.id);

-- ---------------------------------------------------------------------------------------------
-- Vacaciones: periodos ganados, solicitudes en varios estados y permisos
-- ---------------------------------------------------------------------------------------------
INSERT INTO periodo_vacacional (id, tenant_id, empleado_id, desde, hasta, dias_ganados, dias_gozados, dias_vendidos) VALUES
    ('9a9a9a9a-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', DATE '2023-02-01', DATE '2024-01-31', 30, 30, 0),
    ('9a9a9a9a-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', DATE '2024-02-01', DATE '2025-01-31', 30, 15, 0),
    ('9a9a9a9a-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', DATE '2025-02-01', DATE '2026-01-31', 30,  0, 0),
    ('9a9a9a9a-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000003', DATE '2025-03-01', DATE '2026-02-28', 30,  0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO solicitud_vacaciones (id, tenant_id, empleado_id, periodo_id, desde, hasta, dias, estado, motivo_rechazo) VALUES
    ('5a5a5a5a-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', '9a9a9a9a-0000-4000-8000-000000000001', DATE '2023-12-04', DATE '2024-01-02', 30, 'FINALIZADA',    NULL),
    ('5a5a5a5a-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', '9a9a9a9a-0000-4000-8000-000000000002', DATE '2025-08-04', DATE '2025-08-18', 15, 'FINALIZADA',    NULL),
    ('5a5a5a5a-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', '9a9a9a9a-0000-4000-8000-000000000003', (date_trunc('month', current_date) + interval '1 month')::date + 9, (date_trunc('month', current_date) + interval '1 month')::date + 18, 10, 'SOLICITADA',    NULL),
    ('5a5a5a5a-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000003', '9a9a9a9a-0000-4000-8000-000000000004', (date_trunc('month', current_date) + interval '2 months')::date + 4, (date_trunc('month', current_date) + interval '2 months')::date + 18, 15, 'APROBADA_JEFE', NULL),
    ('5a5a5a5a-0000-4000-8000-000000000005', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000001', '9a9a9a9a-0000-4000-8000-000000000003', (date_trunc('month', current_date) + interval '3 months')::date, (date_trunc('month', current_date) + interval '3 months')::date + 6,  7, 'RECHAZADA',     'Coincide con la campaña de ventas')
ON CONFLICT (id) DO NOTHING;

INSERT INTO permiso (id, tenant_id, empleado_id, tipo, desde, hasta, con_goce, motivo, estado, motivo_rechazo) VALUES
    ('beefbeef-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000004', 'MEDICO',   current_date - 20, current_date - 19, true,  'Descanso médico',        'APROBADO',   NULL),
    ('beefbeef-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'dddddddd-0000-4000-8000-000000000005', 'PERSONAL', current_date + 5,  current_date + 5,  false, 'Trámite en el registro civil', 'SOLICITADO', NULL)
ON CONFLICT (id) DO NOTHING;
