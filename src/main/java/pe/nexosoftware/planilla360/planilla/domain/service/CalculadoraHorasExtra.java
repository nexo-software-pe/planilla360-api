package pe.nexosoftware.planilla360.planilla.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/**
 * Pago de horas extra aprobadas. Valor hora = sueldo básico / 30 / horas de la jornada diaria; las dos primeras horas
 * de cada día llevan la primera sobretasa (25 %) y las siguientes la segunda (35 %). Cada día se redondea una sola vez.
 */
public final class CalculadoraHorasExtra {

    private static final int DIAS_COMERCIALES = 30;
    private static final BigDecimal HORAS_CON_PRIMERA_SOBRETASA = BigDecimal.valueOf(2);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public Dinero montoDelDia(Dinero basico, int horasJornadaDiaria, BigDecimal horas, BigDecimal sobretasa1Pct,
            BigDecimal sobretasa2Pct) {
        if (basico.esNegativo()) {
            throw new IllegalArgumentException("El sueldo básico no puede ser negativo");
        }
        if (horasJornadaDiaria <= 0) {
            throw new IllegalArgumentException("La jornada diaria debe ser positiva");
        }
        if (horas.signum() < 0) {
            throw new IllegalArgumentException("Las horas extra no pueden ser negativas");
        }
        BigDecimal conPrimeraTasa = horas.min(HORAS_CON_PRIMERA_SOBRETASA);
        BigDecimal conSegundaTasa = horas.subtract(conPrimeraTasa);
        BigDecimal factorHoras = conPrimeraTasa.multiply(CIEN.add(sobretasa1Pct))
                .add(conSegundaTasa.multiply(CIEN.add(sobretasa2Pct)));
        BigDecimal divisor = BigDecimal.valueOf((long) DIAS_COMERCIALES * horasJornadaDiaria).multiply(CIEN);
        return Dinero.de(basico.monto().multiply(factorHoras).divide(divisor, 2, RoundingMode.HALF_UP));
    }

    public Dinero total(Dinero basico, int horasJornadaDiaria, List<BigDecimal> horasPorDia, BigDecimal sobretasa1Pct,
            BigDecimal sobretasa2Pct) {
        Dinero total = Dinero.CERO;
        for (BigDecimal horas : horasPorDia) {
            total = total.sumar(montoDelDia(basico, horasJornadaDiaria, horas, sobretasa1Pct, sobretasa2Pct));
        }
        return total;
    }
}
