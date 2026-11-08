package pe.nexosoftware.planilla360.planilla.domain.service;

import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosMensualesTrabajador;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/**
 * Template Method del cálculo mensual de un trabajador: básico proporcional a los días laborados, asignación
 * familiar, horas extra, descuentos de asistencia, pensión (según su estrategia) y retención de 5.ª categoría.
 * La gratificación y la CTS no entran aquí: se pagan en sus propias planillas.
 */
public final class CalculadoraLineaMensual {

    private final CalculadoraAsignacionFamiliar asignacionFamiliar = new CalculadoraAsignacionFamiliar();
    private final CalculadoraHorasExtra horasExtra = new CalculadoraHorasExtra();
    private final CalculadoraDescuentoAsistencia descuentoAsistencia = new CalculadoraDescuentoAsistencia();
    private final CalculadoraGratificacion gratificacion = new CalculadoraGratificacion();
    private final CalculadoraRenta5ta renta5ta = new CalculadoraRenta5ta();

    /**
     * @param sobretasaHoraExtra1Pct sobretasa de las dos primeras horas extra del día (25)
     * @param sobretasaHoraExtra2Pct sobretasa de las horas siguientes (35)
     */
    public LineaPlanilla calcular(DatosMensualesTrabajador datos, Periodo periodo, ParametrosLegales parametros,
            BigDecimal sobretasaHoraExtra1Pct, BigDecimal sobretasaHoraExtra2Pct) {
        throw new UnsupportedOperationException("pendiente");
    }

    private Dinero unused() {
        return Dinero.CERO;
    }
}
