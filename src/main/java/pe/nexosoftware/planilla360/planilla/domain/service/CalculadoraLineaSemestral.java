package pe.nexosoftware.planilla360.planilla.domain.service;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosBeneficioSemestral;
import pe.nexosoftware.planilla360.planilla.domain.model.DepositoCts;
import pe.nexosoftware.planilla360.planilla.domain.model.Gratificacion;
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
        Dinero asignacion = asignacionFamiliar.calcular(datos.hijos(), periodo.fin(), parametros);
        Gratificacion calculada = gratificacion.calcular(datos.sueldoBasico(), asignacion,
                datos.horasExtraDelSemestre(), datos.tiempo().meses(), datos.regimenLaboral(), parametros);
        return LineaPlanilla.de(datos.empleadoId())
                .basico(Dinero.CERO)
                .gratificacion(calculada)
                .regimenPensionario(datos.regimenPensionario())
                .build(parametros);
    }

    public LineaPlanilla cts(DatosBeneficioSemestral datos, Periodo periodo, ParametrosLegales parametros) {
        Dinero asignacion = asignacionFamiliar.calcular(datos.hijos(), periodo.fin(), parametros);
        DepositoCts deposito = cts.calcular(datos.sueldoBasico(), asignacion, datos.horasExtraDelSemestre(),
                datos.ultimaGratificacion(), datos.tiempo().meses(), datos.tiempo().dias(), datos.regimenLaboral());
        return LineaPlanilla.de(datos.empleadoId())
                .basico(Dinero.CERO)
                .cts(deposito.monto())
                .regimenPensionario(datos.regimenPensionario())
                .build(parametros);
    }
}
