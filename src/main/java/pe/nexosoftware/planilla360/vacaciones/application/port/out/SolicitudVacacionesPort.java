package pe.nexosoftware.planilla360.vacaciones.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import pe.nexosoftware.planilla360.vacaciones.domain.model.SolicitudVacaciones;

public interface SolicitudVacacionesPort {

    SolicitudVacaciones guardar(SolicitudVacaciones solicitud);

    Optional<SolicitudVacaciones> buscar(UUID id);

    /** Solicitudes que todavía reservan días (solicitada, aprobadas o en goce) del empleado. */
    List<SolicitudVacaciones> activasDelEmpleado(UUID empleadoId);

    /** Solicitudes de los empleados indicados ({@code null} = todos), opcionalmente por estado. */
    List<SolicitudVacaciones> listar(Collection<UUID> empleadoIds, EstadoSolicitud estado);

    /** Aprobadas por RRHH cuyo primer día ya llegó. */
    List<SolicitudVacaciones> porIniciarGoce(LocalDate hoy);

    /** En goce cuyo último día ya pasó. */
    List<SolicitudVacaciones> porFinalizar(LocalDate hoy);
}
