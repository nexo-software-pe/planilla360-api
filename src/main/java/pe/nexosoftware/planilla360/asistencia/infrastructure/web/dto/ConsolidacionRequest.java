package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ConsolidacionRequest(@NotNull LocalDate fecha) {
}
