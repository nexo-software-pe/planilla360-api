package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos de un día que el consolidador necesita: el horario del empleado, si fue feriado, si estaba
 * justificado (vacaciones o permiso) y las marcaciones registradas. El dominio no consulta nada: recibe
 * todo como dato.
 */
public record ContextoDia(
        LocalDate fecha, Horario horario, boolean feriado, boolean justificado, List<Marcacion> marcaciones) {
}
