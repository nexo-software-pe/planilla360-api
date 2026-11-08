package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AsignarHorarioRequest(@NotNull UUID empleadoId, @NotNull LocalDate desde) {
}
