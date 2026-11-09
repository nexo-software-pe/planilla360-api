package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Planilla sin sus líneas. {@code periodo} va como {@code yyyy-MM}. */
public record PlanillaResponse(
        UUID id,
        String periodo,
        TipoPlanilla tipo,
        EstadoPlanilla estado,
        int trabajadores,
        BigDecimal totalIngresos,
        BigDecimal totalDescuentos,
        BigDecimal totalNeto,
        BigDecimal totalEssalud,
        Instant cerradaEn) {
}
