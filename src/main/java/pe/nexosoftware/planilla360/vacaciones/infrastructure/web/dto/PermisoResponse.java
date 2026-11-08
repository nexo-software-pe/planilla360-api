package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.TipoPermiso;

public record PermisoResponse(
        UUID id,
        UUID empleadoId,
        TipoPermiso tipo,
        LocalDate desde,
        LocalDate hasta,
        int dias,
        boolean conGoce,
        String motivo,
        EstadoPermiso estado,
        String motivoRechazo) {
}
