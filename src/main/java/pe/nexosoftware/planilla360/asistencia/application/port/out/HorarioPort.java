package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;

public interface HorarioPort {

    record HorarioGuardado(UUID id, String nombre, Horario horario) {
    }

    HorarioGuardado guardar(String nombre, Horario horario);

    List<HorarioGuardado> listar();

    boolean existe(UUID horarioId);

    boolean existeNombre(String nombre);

    /** Asigna el horario desde la fecha; cierra la asignación anterior que siguiera abierta. */
    void asignar(UUID empleadoId, UUID horarioId, LocalDate desde);

    /** Horario que tenía cada empleado en la fecha (los que no tienen horario no aparecen). */
    Map<UUID, Horario> vigentesEn(LocalDate fecha);
}
