package pe.nexosoftware.planilla360.planilla.domain.regimen;

import java.math.BigDecimal;

/** Tarifas vigentes de una AFP (datos del catálogo). */
public record AfpVigente(String nombre, BigDecimal comisionFlujoPct, BigDecimal comisionMixtaPct) {

    public BigDecimal comision(TipoComision tipo) {
        return switch (tipo) {
            case FLUJO -> comisionFlujoPct;
            case MIXTA -> comisionMixtaPct;
        };
    }
}
