package pe.nexosoftware.planilla360.planilla.domain.service;

/** Convierte la jornada semanal del contrato en horas por día (semana de 5 días, tope de 8 horas diarias). */
public final class JornadaLaboral {

    private static final int DIAS_POR_SEMANA = 5;
    private static final int MAXIMO_HORAS_DIARIAS = 8;
    private static final int MAXIMO_HORAS_SEMANALES = 48;

    private JornadaLaboral() {
    }

    public static int horasDiarias(int jornadaSemanal) {
        if (jornadaSemanal < 1 || jornadaSemanal > MAXIMO_HORAS_SEMANALES) {
            throw new IllegalArgumentException("La jornada semanal debe estar entre 1 y 48 horas: " + jornadaSemanal);
        }
        int porDia = (jornadaSemanal + DIAS_POR_SEMANA - 1) / DIAS_POR_SEMANA;
        return Math.min(porDia, MAXIMO_HORAS_DIARIAS);
    }
}
