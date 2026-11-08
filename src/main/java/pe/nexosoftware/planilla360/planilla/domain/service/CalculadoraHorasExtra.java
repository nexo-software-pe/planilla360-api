package pe.nexosoftware.planilla360.planilla.domain.service;

import java.math.BigDecimal;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/**
 * Pago de horas extra aprobadas. Valor hora = sueldo básico / 30 / horas de la jornada diaria; las dos primeras horas
 * de cada día llevan la primera sobretasa (25 %) y las siguientes la segunda (35 %).
 */
public final class CalculadoraHorasExtra {

    public Dinero montoDelDia(Dinero basico, int horasJornadaDiaria, BigDecimal horas, BigDecimal sobretasa1Pct,
            BigDecimal sobretasa2Pct) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero total(Dinero basico, int horasJornadaDiaria, List<BigDecimal> horasPorDia, BigDecimal sobretasa1Pct,
            BigDecimal sobretasa2Pct) {
        throw new UnsupportedOperationException("pendiente");
    }
}
