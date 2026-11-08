package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import java.time.LocalDate;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia;

public record AsistenciaDiariaResponse(
        LocalDate fecha, int minutosTardanza, int minutosExtra, EstadoAsistencia estado) {
}
