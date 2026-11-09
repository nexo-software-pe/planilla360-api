package pe.nexosoftware.planilla360.vacaciones.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.Permiso;

public interface PermisoPort {

    Permiso guardar(Permiso permiso);

    Optional<Permiso> buscar(UUID id);

    /** Permisos sin goce ya aprobados cuyos días se solapan con el rango. */
    List<Permiso> aprobadosSinGoceQueSolapan(LocalDate desde, LocalDate hasta);

    /** Permisos de los empleados indicados ({@code null} = todos), opcionalmente por estado. */
    List<Permiso> listar(Collection<UUID> empleadoIds, EstadoPermiso estado);
}
