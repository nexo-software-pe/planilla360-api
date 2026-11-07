-- V2: datos referenciales necesarios en todos los entornos.
-- Los valores legales son parámetros versionados por vigencia (se ajustan por migración o por API).

INSERT INTO parametro_legal (codigo, descripcion, valor, vigente_desde) VALUES
    ('RMV',                  'Remuneración mínima vital (S/)',                         1130.0000, DATE '2025-01-01'),
    ('UIT',                  'Unidad impositiva tributaria (S/)',                      5350.0000, DATE '2025-01-01'),
    ('ASIG_FAMILIAR_PCT',    'Asignación familiar (% de la RMV)',                        10.0000, DATE '2025-01-01'),
    ('ONP_PCT',              'Aporte al Sistema Nacional de Pensiones (%)',              13.0000, DATE '2025-01-01'),
    ('AFP_APORTE_PCT',       'AFP: aporte obligatorio al fondo (%)',                     10.0000, DATE '2025-01-01'),
    ('AFP_PRIMA_PCT',        'AFP: prima de seguro (%)',                                  1.3700, DATE '2025-01-01'),
    ('ESSALUD_PCT',          'EsSalud: aporte del empleador (%)',                         9.0000, DATE '2025-01-01'),
    ('HE_SOBRETASA_1_PCT',   'Horas extra: sobretasa primeras 2 horas (%)',              25.0000, DATE '2025-01-01'),
    ('HE_SOBRETASA_2_PCT',   'Horas extra: sobretasa desde la 3.ª hora (%)',             35.0000, DATE '2025-01-01'),
    ('JORNADA_MAX_DIA_H',    'Jornada máxima diaria (horas)',                             8.0000, DATE '2025-01-01'),
    ('JORNADA_MAX_SEMANA_H', 'Jornada máxima semanal (horas)',                           48.0000, DATE '2025-01-01'),
    ('BONIF_EXTRAORDINARIA_PCT', 'Bonificación extraordinaria sobre gratificación (%)',   9.0000, DATE '2025-01-01');

-- Comisiones ficticias, solo para fines de aprendizaje.
INSERT INTO afp (nombre, comision_flujo, comision_mixta, vigente_desde) VALUES
    ('Prima',     1.60, 1.25, DATE '2025-01-01'),
    ('Integra',   1.55, 1.20, DATE '2025-01-01'),
    ('Profuturo', 1.69, 1.20, DATE '2025-01-01'),
    ('Habitat',   1.47, 1.25, DATE '2025-01-01');

INSERT INTO feriado (fecha, descripcion, ambito) VALUES
    (DATE '2026-01-01', 'Año Nuevo',                          'NACIONAL'),
    (DATE '2026-04-02', 'Jueves Santo',                       'NACIONAL'),
    (DATE '2026-04-03', 'Viernes Santo',                      'NACIONAL'),
    (DATE '2026-05-01', 'Día del Trabajo',                    'NACIONAL'),
    (DATE '2026-06-07', 'Batalla de Arica y Día de la Bandera', 'NACIONAL'),
    (DATE '2026-06-29', 'San Pedro y San Pablo',              'NACIONAL'),
    (DATE '2026-07-23', 'Día de la Fuerza Aérea del Perú',    'NACIONAL'),
    (DATE '2026-07-28', 'Fiestas Patrias',                    'NACIONAL'),
    (DATE '2026-07-29', 'Fiestas Patrias',                    'NACIONAL'),
    (DATE '2026-08-06', 'Batalla de Junín',                   'NACIONAL'),
    (DATE '2026-08-30', 'Santa Rosa de Lima',                 'NACIONAL'),
    (DATE '2026-10-08', 'Combate de Angamos',                 'NACIONAL'),
    (DATE '2026-11-01', 'Todos los Santos',                   'NACIONAL'),
    (DATE '2026-12-08', 'Inmaculada Concepción',              'NACIONAL'),
    (DATE '2026-12-09', 'Batalla de Ayacucho',                'NACIONAL'),
    (DATE '2026-12-25', 'Navidad',                            'NACIONAL');
