package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.TipoPermiso;

/** {@code empleadoId} solo lo indica RRHH; un empleado siempre solicita para sí. */
public record PermisoRequest(
        UUID empleadoId,
        @NotNull TipoPermiso tipo,
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta,
        boolean conGoce,
        @Size(max = 300) String motivo) {
}
