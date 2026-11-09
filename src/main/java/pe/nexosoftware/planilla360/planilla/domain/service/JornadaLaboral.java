package pe.nexosoftware.planilla360.planilla.domain.service;

/**
 * Horas de la jornada diaria de un trabajador, con tope de 8 horas. Se deriva del horario que tiene asignado
 * (salida - entrada - refrigerio) y, si no tiene horario, de la jornada semanal de su contrato dividida entre sus
 * días laborables. Las fracciones de hora se redondean hacia arriba.
 */
public final class JornadaLaboral {

    private static final int MINUTOS_POR_HORA = 60;
    private static final int MAXIMO_HORAS_DIARIAS = 8;
    private static final int MAXIMO_HORAS_SEMANALES = 48;
    private static final int MAXIMO_DIAS_LABORABLES = 7;

    private JornadaLaboral() {
    }

    /**
     * @param minutosDeHorario minutos efectivos por día del horario asignado; {@code null} si no tiene horario
     * @param jornadaSemanal horas semanales del contrato (solo se usa sin horario)
     * @param diasLaborables días laborables por semana del contrato (solo se usa sin horario)
     */
    public static int horasDiarias(Integer minutosDeHorario, int jornadaSemanal, int diasLaborables) {
        if (minutosDeHorario != null) {
            if (minutosDeHorario < 1) {
                throw new IllegalArgumentException(
                        "El horario debe tener al menos un minuto efectivo por día: " + minutosDeHorario);
            }
            return topeDiario(redondearHaciaArriba(minutosDeHorario, MINUTOS_POR_HORA));
        }
        if (jornadaSemanal < 1 || jornadaSemanal > MAXIMO_HORAS_SEMANALES) {
            throw new IllegalArgumentException("La jornada semanal debe estar entre 1 y 48 horas: " + jornadaSemanal);
        }
        if (diasLaborables < 1 || diasLaborables > MAXIMO_DIAS_LABORABLES) {
            throw new IllegalArgumentException("Los días laborables deben estar entre 1 y 7: " + diasLaborables);
        }
        return topeDiario(redondearHaciaArriba(jornadaSemanal, diasLaborables));
    }

    private static int redondearHaciaArriba(int dividendo, int divisor) {
        return (dividendo + divisor - 1) / divisor;
    }

    private static int topeDiario(int horas) {
        return Math.min(horas, MAXIMO_HORAS_DIARIAS);
    }
}
