package pe.nexosoftware.planilla360.planilla.domain.service;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Descuentos por tardanzas e inasistencias injustificadas: esqueleto, se construye test-first. */
public final class CalculadoraDescuentoAsistencia {

    public Dinero porTardanza(Dinero basico, int minutosTardanza, int horasJornadaDiaria) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero porInasistencias(Dinero basico, int diasInjustificados) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero total(Dinero basico, int minutosTardanza, int diasInjustificados, int horasJornadaDiaria) {
        throw new UnsupportedOperationException("pendiente");
    }
}
