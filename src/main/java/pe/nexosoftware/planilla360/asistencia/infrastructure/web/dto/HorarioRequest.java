package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record HorarioRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull LocalTime entrada,
        @NotNull LocalTime salida,
        @NotNull Integer toleranciaMin,
        @NotEmpty Set<DayOfWeek> dias) {
}
