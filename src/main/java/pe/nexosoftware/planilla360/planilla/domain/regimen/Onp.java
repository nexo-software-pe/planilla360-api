package pe.nexosoftware.planilla360.planilla.domain.regimen;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/** Sistema Nacional de Pensiones. Esqueleto. */
public record Onp() implements RegimenPensionario {

    @Override
    public String nombre() {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public DescuentoPension calcular(Dinero remuneracionAsegurable, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }
}
