-- V7: datos para derivar la jornada diaria de la planilla (valor de la hora y de las horas extra).
-- El horario asignado define la jornada (salida - entrada - refrigerio); sin horario se usa la jornada semanal
-- del contrato dividida entre sus días laborables.

ALTER TABLE horario
    ADD COLUMN refrigerio_min integer NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_horario_refrigerio CHECK (refrigerio_min >= 0);

ALTER TABLE contrato
    ADD COLUMN dias_laborables integer NOT NULL DEFAULT 5,
    ADD CONSTRAINT ck_contrato_dias_laborables CHECK (dias_laborables BETWEEN 1 AND 7);
