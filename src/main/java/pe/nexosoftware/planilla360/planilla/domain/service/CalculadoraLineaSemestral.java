package pe.nexosoftware.planilla360.planilla.domain.service;

import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosBeneficioSemestral;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/**
 * Líneas de las planillas de gratificación y de CTS. Ninguna paga básico: el beneficio es el único ingreso, no sufre
 * descuento de pensión (la base asegurable es cero) y la renta de 5.ª ya se proyectó en las planillas mensuales.
 */
public final class CalculadoraLineaSemestral {

    private final CalculadoraAsignacionFamiliar asignacionFamiliar = new CalculadoraAsignacionFamiliar();
    private final CalculadoraGratificacion gratificacion = new CalculadoraGratificacion();
    private final CalculadoraCts cts = new CalculadoraCts(gratificacion);

    public LineaPlanilla gratificacion(DatosBeneficioSemestral datos, Periodo periodo, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }

    public LineaPlanilla cts(DatosBeneficioSemestral datos, Periodo periodo, ParametrosLegales parametros) {
        throw new UnsupportedOperationException("pendiente");
    }
}
