package pe.nexosoftware.planilla360.planilla.domain.regimen;

import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/** Sistema privado de pensiones con la comisión de una AFP concreta. Esqueleto. */
public record Afp(String administradora, BigDecimal comisionPct) implements RegimenPensionario {

    @Override
    public String nombre() {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public DescuentoPension calcular(Dinero remuneracionAsegurable, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }
}
