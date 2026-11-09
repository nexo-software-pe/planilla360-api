package pe.nexosoftware.planilla360.boleta.dto;

import java.util.UUID;

/**
 * Avance de la generación masiva de boletas de una planilla.
 *
 * @param estado SIN_INICIAR, EN_PROCESO, COMPLETADA o CON_FALLOS
 */
public record EstadoGeneracionResponse(
        UUID planillaId,
        String estado,
        long total,
        long pendientes,
        long generadas,
        long enviadas,
        long fallidas) {
}
