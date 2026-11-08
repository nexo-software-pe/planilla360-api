package pe.nexosoftware.planilla360.asistencia.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.application.model.Actor;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import pe.nexosoftware.planilla360.asistencia.domain.model.HoraExtra;

public interface GestionarHorasExtraUseCase {

    /**
     * Registra una solicitud. Un empleado solo la registra para sí; RRHH puede registrarla para un
     * empleado indicado.
     */
    HoraExtra solicitar(Actor actor, UUID empleadoId, LocalDate fecha, BigDecimal horas, String motivo);

    /** Aprueba o rechaza: lo hace RRHH, el administrador de la empresa o el jefe directo del empleado. */
    HoraExtra resolver(Actor actor, UUID id, boolean aprobar);

    /** RRHH ve todas; un empleado ve las propias y las de su equipo directo. */
    List<HoraExtra> listar(Actor actor, EstadoHoraExtra estado);
}
