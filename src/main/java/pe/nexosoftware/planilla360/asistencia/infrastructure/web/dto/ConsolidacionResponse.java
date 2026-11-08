package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import java.time.LocalDate;

public record ConsolidacionResponse(LocalDate fecha, int empleadosProcesados) {
}
