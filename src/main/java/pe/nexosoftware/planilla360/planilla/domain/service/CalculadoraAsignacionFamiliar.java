package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.LocalDate;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/** Asignación familiar: esqueleto, se construye test-first. */
public final class CalculadoraAsignacionFamiliar {

    public int hijosElegibles(List<Hijo> hijos, LocalDate al) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero calcular(List<Hijo> hijos, LocalDate al, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }
}
