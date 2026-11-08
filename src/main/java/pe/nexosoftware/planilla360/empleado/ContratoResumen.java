package pe.nexosoftware.planilla360.empleado;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vista pública del contrato vigente de un empleado. El régimen pensionario es {@code ONP} o {@code AFP}; la AFP y el
 * tipo de comisión ({@code FLUJO} o {@code MIXTA}) solo existen en el régimen AFP.
 */
public record ContratoResumen(
        UUID empleadoId,
        BigDecimal sueldoBasico,
        int jornadaSemanal,
        String regimenPensionario,
        UUID afpId,
        String tipoComision) {
}
