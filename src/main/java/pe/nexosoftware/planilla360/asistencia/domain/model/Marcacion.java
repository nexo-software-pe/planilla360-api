package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.time.Instant;

/** Registro de entrada o salida de un empleado en un instante exacto. */
public record Marcacion(Instant instante, TipoMarcacion tipo) {
}
