package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.time.LocalDate;

/** Asistencia consolidada de un empleado en una fecha: minutos de tardanza, minutos extra y estado. */
public record AsistenciaDiaria(LocalDate fecha, int minutosTardanza, int minutosExtra, EstadoAsistencia estado) {
}
