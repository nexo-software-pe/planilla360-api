package pe.nexosoftware.planilla360.auditoria.dto;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import tools.jackson.databind.JsonNode;

public record AuditoriaResponse(
        UUID id,
        UUID tenantId,
        String entidad,
        UUID entidadId,
        Accion accion,
        String usuario,
        Instant instante,
        JsonNode antes,
        JsonNode despues) {
}
