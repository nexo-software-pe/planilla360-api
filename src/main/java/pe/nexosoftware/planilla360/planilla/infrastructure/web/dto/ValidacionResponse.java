package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import java.util.List;

/** Incidencias que impiden cerrar la planilla (no son un estado de la planilla). */
public record ValidacionResponse(boolean valida, List<IncidenciaResponse> incidencias) {

    public record IncidenciaResponse(String codigo, String mensaje) {
    }
}
