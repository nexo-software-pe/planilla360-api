package pe.nexosoftware.planilla360.empleado.model;

import java.math.BigDecimal;

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
        throw new UnsupportedOperationException("pendiente");
    }
}
