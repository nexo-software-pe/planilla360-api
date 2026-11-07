package pe.nexosoftware.planilla360.planilla.domain.regimen;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/** Sistema Nacional de Pensiones: un único aporte porcentual (13 % de referencia). */
public record Onp() implements RegimenPensionario {

    @Override
    public String nombre() {
        return "ONP";
    }

    @Override
    public DescuentoPension calcular(Dinero remuneracionAsegurable, ParametrosLegales parametros) {
        Dinero base = BaseAsegurable.exigirValida(remuneracionAsegurable);
        return new DescuentoPension(base.porcentaje(parametros.pctOnp()), Dinero.CERO, Dinero.CERO);
    }
}
