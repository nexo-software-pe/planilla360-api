package pe.nexosoftware.planilla360.planilla.domain.service;

import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.Gratificacion;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

/** Gratificación de julio y diciembre: esqueleto, se construye test-first. */
public final class CalculadoraGratificacion {

    public boolean esMesDeGratificacion(Periodo periodo) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero remuneracionComputable(Dinero basico, Dinero asignacionFamiliar, List<Dinero> horasExtraDelSemestre) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Gratificacion calcular(Dinero basico, Dinero asignacionFamiliar, List<Dinero> horasExtraDelSemestre,
            int mesesCompletos, RegimenLaboral regimen, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }
}
