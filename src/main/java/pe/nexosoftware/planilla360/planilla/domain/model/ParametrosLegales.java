package pe.nexosoftware.planilla360.planilla.domain.model;

import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Parámetros legales vigentes para el cálculo. Esqueleto: las validaciones se construyen test-first. */
public record ParametrosLegales(
        Dinero rmv,
        Dinero uit,
        BigDecimal pctAsignacionFamiliar,
        BigDecimal pctOnp,
        BigDecimal pctAfpAporte,
        BigDecimal pctAfpPrima,
        BigDecimal pctEssalud,
        BigDecimal pctBonificacionExtraordinaria) {
}
