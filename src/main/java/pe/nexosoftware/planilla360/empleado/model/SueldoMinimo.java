package pe.nexosoftware.planilla360.empleado.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sueldo mínimo legal de un contrato según su jornada semanal: con 24 horas o más es la RMV completa;
 * con menos (tiempo parcial) es la RMV proporcional a la jornada sobre 48 horas.
 */
public final class SueldoMinimo {

    public static final int JORNADA_COMPLETA_MINIMA = 24;
    public static final int JORNADA_MAXIMA = 48;

    private SueldoMinimo() {
    }

    public static BigDecimal para(BigDecimal rmv, int jornadaSemanal) {
        if (jornadaSemanal < 1 || jornadaSemanal > JORNADA_MAXIMA) {
            throw new IllegalArgumentException("La jornada semanal debe estar entre 1 y 48 horas.");
        }
        if (jornadaSemanal >= JORNADA_COMPLETA_MINIMA) {
            return rmv;
        }
        return rmv.multiply(BigDecimal.valueOf(jornadaSemanal))
                .divide(BigDecimal.valueOf(JORNADA_MAXIMA), 2, RoundingMode.HALF_UP);
    }
}
