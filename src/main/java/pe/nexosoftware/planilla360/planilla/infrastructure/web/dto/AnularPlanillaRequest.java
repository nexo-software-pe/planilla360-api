package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularPlanillaRequest(@NotBlank @Size(max = 300) String motivo) {
}
