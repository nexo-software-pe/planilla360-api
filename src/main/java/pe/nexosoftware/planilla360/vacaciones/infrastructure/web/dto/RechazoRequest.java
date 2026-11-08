package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazoRequest(@NotBlank @Size(max = 300) String motivo) {
}
