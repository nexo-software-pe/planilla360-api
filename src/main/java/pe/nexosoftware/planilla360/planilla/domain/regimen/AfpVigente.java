package pe.nexosoftware.planilla360.planilla.domain.regimen;

import java.math.BigDecimal;

/** Tarifas vigentes de una AFP (datos del catálogo). Esqueleto. */
public record AfpVigente(String nombre, BigDecimal comisionFlujoPct, BigDecimal comisionMixtaPct) {

    public BigDecimal comision(TipoComision tipo) {
        throw new UnsupportedOperationException("pendiente");
    }
}
