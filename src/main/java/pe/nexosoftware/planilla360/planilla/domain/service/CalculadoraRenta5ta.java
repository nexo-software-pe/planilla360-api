package pe.nexosoftware.planilla360.planilla.domain.service;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/** Retención de renta de 5.ª categoría (simplificada): esqueleto, se construye test-first. */
public final class CalculadoraRenta5ta {

    public Dinero impuestoAnual(Dinero rentaBrutaAnual, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero retencionMensual(Dinero remuneracionMensual, int mesesRestantes, Dinero gratificacionesProyectadas,
            Dinero ingresosYaPercibidos, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }
}
