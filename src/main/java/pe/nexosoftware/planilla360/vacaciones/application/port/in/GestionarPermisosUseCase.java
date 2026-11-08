package pe.nexosoftware.planilla360.vacaciones.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.Permiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.TipoPermiso;

public interface GestionarPermisosUseCase {

    /** Un empleado solicita para sí; RRHH puede registrarlo para el empleado indicado. */
    Permiso solicitar(
            Actor actor,
            UUID empleadoId,
            TipoPermiso tipo,
            LocalDate desde,
            LocalDate hasta,
            boolean conGoce,
            String motivo);

    Permiso aprobar(Actor actor, UUID permisoId);

    Permiso rechazar(Actor actor, UUID permisoId, String motivo);

    /** RRHH ve todos; un empleado ve los propios. */
    List<Permiso> listar(Actor actor, EstadoPermiso estado);
}
