package pe.nexosoftware.planilla360.vacaciones.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.vacaciones.VacacionesApi;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PermisoPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.SolicitudVacacionesPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.Permiso;

/** Respuestas de vacaciones y permisos para otros módulos (la planilla las usa para validar y descontar). */
@Service
@Transactional(readOnly = true)
public class AusenciasService implements VacacionesApi {

    private final SolicitudVacacionesPort solicitudes;
    private final PermisoPort permisos;

    public AusenciasService(SolicitudVacacionesPort solicitudes, PermisoPort permisos) {
        this.solicitudes = solicitudes;
        this.permisos = permisos;
    }

    @Override
    public int solicitudesPendientesQueSolapan(LocalDate desde, LocalDate hasta) {
        return solicitudes.pendientesDeAprobacionQueSolapan(desde, hasta).size();
    }

    @Override
    public Map<UUID, Integer> diasDePermisoSinGoce(LocalDate desde, LocalDate hasta) {
        Map<UUID, Integer> dias = new HashMap<>();
        for (Permiso permiso : permisos.aprobadosSinGoceQueSolapan(desde, hasta)) {
            LocalDate inicio = permiso.desde().isAfter(desde) ? permiso.desde() : desde;
            LocalDate fin = permiso.hasta().isBefore(hasta) ? permiso.hasta() : hasta;
            int dentro = (int) ChronoUnit.DAYS.between(inicio, fin) + 1;
            dias.merge(permiso.empleadoId(), dentro, Integer::sum);
        }
        return dias;
    }
}
