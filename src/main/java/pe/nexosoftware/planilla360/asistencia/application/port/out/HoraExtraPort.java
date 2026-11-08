package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import pe.nexosoftware.planilla360.asistencia.domain.model.HoraExtra;

public interface HoraExtraPort {

    HoraExtra guardar(HoraExtra horaExtra);

    Optional<HoraExtra> buscar(UUID id);

    /** Solicitudes de los empleados indicados ({@code null} = todos), opcionalmente por estado. */
    List<HoraExtra> listar(Collection<UUID> empleadoIds, EstadoHoraExtra estado);
}
