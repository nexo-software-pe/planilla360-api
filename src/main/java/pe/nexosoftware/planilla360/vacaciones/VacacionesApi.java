package pe.nexosoftware.planilla360.vacaciones;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** API pública del módulo de vacaciones y permisos. Las consultas se limitan a la empresa del contexto. */
public interface VacacionesApi {

    /**
     * Solicitudes de vacaciones todavía sin aprobación final de RRHH (solicitadas o aprobadas solo por el jefe) cuyos
     * días se solapan con el rango.
     */
    int solicitudesPendientesQueSolapan(LocalDate desde, LocalDate hasta);

    /** Días de permiso sin goce aprobados dentro del rango, por empleado (solo los días que caen en el rango). */
    Map<UUID, Integer> diasDePermisoSinGoce(LocalDate desde, LocalDate hasta);
}
