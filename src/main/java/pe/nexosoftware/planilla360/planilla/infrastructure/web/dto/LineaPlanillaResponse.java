package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Boleta de un trabajador: sus montos y el desglose por concepto en orden de impresión. */
public record LineaPlanillaResponse(
        UUID empleadoId,
        BigDecimal basico,
        BigDecimal asignacionFamiliar,
        BigDecimal horasExtra,
        BigDecimal bonos,
        BigDecimal gratificacion,
        BigDecimal bonificacionExtraordinaria,
        BigDecimal cts,
        String regimenPensionario,
        BigDecimal descuentoPension,
        BigDecimal retencion5ta,
        BigDecimal descuentoAsistencia,
        BigDecimal otrosDescuentos,
        BigDecimal aporteEssalud,
        BigDecimal totalIngresos,
        BigDecimal totalDescuentos,
        BigDecimal neto,
        List<ConceptoResponse> conceptos) {
}
