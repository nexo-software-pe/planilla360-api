package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Resultado de simular una gratificación o una CTS: no tiene identificador porque no se guardó nada. */
public record SimulacionResponse(
        String periodo,
        TipoPlanilla tipo,
        int trabajadores,
        BigDecimal totalIngresos,
        BigDecimal totalDescuentos,
        BigDecimal totalNeto,
        BigDecimal totalEssalud,
        List<LineaPlanillaResponse> lineas) {
}
