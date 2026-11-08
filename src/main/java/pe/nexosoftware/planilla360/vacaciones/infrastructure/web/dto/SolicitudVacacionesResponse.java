package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;

public record SolicitudVacacionesResponse(
        UUID id,
        UUID empleadoId,
        UUID periodoId,
        LocalDate desde,
        LocalDate hasta,
        int dias,
        EstadoSolicitud estado,
        String motivoRechazo) {
}
