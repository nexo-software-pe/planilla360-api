package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SolicitudVacacionesRequest(@NotNull LocalDate desde, @NotNull LocalDate hasta) {
}
