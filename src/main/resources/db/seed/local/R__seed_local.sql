-- Datos ficticios SOLO para el perfil local (la ubicación db/seed/local se carga únicamente con
-- spring.profiles.active=local). Es repetible e idempotente. Contraseña de desarrollo: ver README.

INSERT INTO empresa (id, ruc, razon_social, regimen_laboral, plan, activa) VALUES
    ('11111111-1111-4111-8111-111111111111', '20601234565', 'Textiles Andinos S.A.C.',        'GENERAL',         'PROFESIONAL', true),
    ('22222222-2222-4222-8222-222222222222', '20602345671', 'Panadería La Espiga E.I.R.L.',   'PEQUENA_EMPRESA', 'BASICO',      true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO usuario (id, tenant_id, email, nombre, password_hash, rol, activo) VALUES
    ('aaaaaaaa-0000-4000-8000-000000000001', NULL,
     'admin.saas@nexosoftware.pe', 'Administrador Nexo', '$2a$10$cNIdocLUnXmOScTJv9Y4Cul43.H84Eogqr5EDMdZOeLZKQdOLOrtG', 'ADMIN_SAAS', true),
    ('aaaaaaaa-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111',
     'admin@textilesandinos.ejemplo.pe', 'Rosa Quispe Mamani', '$2a$10$cNIdocLUnXmOScTJv9Y4Cul43.H84Eogqr5EDMdZOeLZKQdOLOrtG', 'ADMIN_EMPRESA', true),
    ('aaaaaaaa-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111',
     'rrhh@textilesandinos.ejemplo.pe', 'Carlos Huamán Ríos', '$2a$10$cNIdocLUnXmOScTJv9Y4Cul43.H84Eogqr5EDMdZOeLZKQdOLOrtG', 'RRHH', true),
    ('aaaaaaaa-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111',
     'empleado@textilesandinos.ejemplo.pe', 'Lucía Flores Paredes', '$2a$10$cNIdocLUnXmOScTJv9Y4Cul43.H84Eogqr5EDMdZOeLZKQdOLOrtG', 'EMPLEADO', true),
    ('aaaaaaaa-0000-4000-8000-000000000005', '22222222-2222-4222-8222-222222222222',
     'admin@laespiga.ejemplo.pe', 'Julio Mendoza Vega', '$2a$10$cNIdocLUnXmOScTJv9Y4Cul43.H84Eogqr5EDMdZOeLZKQdOLOrtG', 'ADMIN_EMPRESA', true),
    ('aaaaaaaa-0000-4000-8000-000000000006', '22222222-2222-4222-8222-222222222222',
     'rrhh@laespiga.ejemplo.pe', 'Marta Salazar Cruz', '$2a$10$cNIdocLUnXmOScTJv9Y4Cul43.H84Eogqr5EDMdZOeLZKQdOLOrtG', 'RRHH', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO area (id, tenant_id, nombre) VALUES
    ('bbbbbbbb-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'Producción'),
    ('bbbbbbbb-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'Administración'),
    ('bbbbbbbb-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'Ventas'),
    ('bbbbbbbb-0000-4000-8000-000000000004', '22222222-2222-4222-8222-222222222222', 'Panadería'),
    ('bbbbbbbb-0000-4000-8000-000000000005', '22222222-2222-4222-8222-222222222222', 'Atención al cliente')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cargo (id, tenant_id, area_id, nombre, sueldo_referencial) VALUES
    ('cccccccc-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'bbbbbbbb-0000-4000-8000-000000000001', 'Operario de tejeduría', 1300.00),
    ('cccccccc-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'bbbbbbbb-0000-4000-8000-000000000001', 'Supervisor de planta',   3200.00),
    ('cccccccc-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'bbbbbbbb-0000-4000-8000-000000000002', 'Analista de RRHH',       2800.00),
    ('cccccccc-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'bbbbbbbb-0000-4000-8000-000000000003', 'Ejecutivo de ventas',    2200.00),
    ('cccccccc-0000-4000-8000-000000000005', '22222222-2222-4222-8222-222222222222', 'bbbbbbbb-0000-4000-8000-000000000004', 'Maestro panadero',      1800.00),
    ('cccccccc-0000-4000-8000-000000000006', '22222222-2222-4222-8222-222222222222', 'bbbbbbbb-0000-4000-8000-000000000005', 'Cajero',                1200.00)
ON CONFLICT (id) DO NOTHING;
