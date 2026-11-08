package pe.nexosoftware.planilla360.empleado.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CeseRequest(@NotNull LocalDate fechaCese) {
}
