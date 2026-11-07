package pe.nexosoftware.planilla360.planilla.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/**
 * Parámetros legales vigentes para calcular una planilla (RMV, UIT y tasas en porcentaje, 13 = 13 %).
 * El dominio los recibe como datos: se leen del catálogo (versionados por vigencia) y nunca se escriben en el código.
 */
public record ParametrosLegales(
        Dinero rmv,
        Dinero uit,
        BigDecimal pctAsignacionFamiliar,
        BigDecimal pctOnp,
        BigDecimal pctAfpAporte,
        BigDecimal pctAfpPrima,
        BigDecimal pctEssalud,
        BigDecimal pctBonificacionExtraordinaria) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public ParametrosLegales {
        Objects.requireNonNull(rmv, "La RMV es obligatoria");
        Objects.requireNonNull(uit, "La UIT es obligatoria");
        exigirPositivo(rmv, "RMV");
        exigirPositivo(uit, "UIT");
        exigirPorcentaje(pctAsignacionFamiliar, "asignación familiar");
        exigirPorcentaje(pctOnp, "ONP");
        exigirPorcentaje(pctAfpAporte, "aporte AFP");
        exigirPorcentaje(pctAfpPrima, "prima AFP");
        exigirPorcentaje(pctEssalud, "EsSalud");
        exigirPorcentaje(pctBonificacionExtraordinaria, "bonificación extraordinaria");
    }

    private static void exigirPositivo(Dinero monto, String nombre) {
        if (monto.esNegativo() || monto.esCero()) {
            throw new IllegalArgumentException("La " + nombre + " debe ser mayor que cero");
        }
    }

    private static void exigirPorcentaje(BigDecimal pct, String nombre) {
        Objects.requireNonNull(pct, "El porcentaje de " + nombre + " es obligatorio");
        if (pct.signum() < 0 || pct.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El porcentaje de " + nombre + " debe estar entre 0 y 100: " + pct);
        }
    }
}
