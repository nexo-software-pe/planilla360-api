package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.util.Map;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;

/** Ausencias que afectan la planilla (las provee el módulo de vacaciones). */
public interface ConsultaVacacionesPort {

    /** Solicitudes de vacaciones sin aprobación final de RRHH que se solapan con el periodo. */
    int solicitudesPendientesQueSolapan(Periodo periodo);

    /** Días de permiso sin goce aprobados dentro del periodo, por trabajador. */
    Map<UUID, Integer> diasDePermisoSinGoce(Periodo periodo);
}
