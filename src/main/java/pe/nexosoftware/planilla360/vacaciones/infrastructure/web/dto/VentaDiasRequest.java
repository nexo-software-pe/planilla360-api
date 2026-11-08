package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record VentaDiasRequest(@Min(1) @Max(15) int dias) {
}
