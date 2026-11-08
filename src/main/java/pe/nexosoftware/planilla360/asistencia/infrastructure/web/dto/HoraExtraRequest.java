package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code empleadoId} solo lo indica RRHH; un empleado siempre solicita para sí. */
public record HoraExtraRequest(
        UUID empleadoId, @NotNull LocalDate fecha, @NotNull BigDecimal horas, @Size(max = 200) String motivo) {
}
