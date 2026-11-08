package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.SolicitudVacacionesPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import pe.nexosoftware.planilla360.vacaciones.domain.model.SolicitudVacaciones;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity.SolicitudVacacionesEntity;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.repository.SolicitudVacacionesJpaRepository;

@Component
class SolicitudVacacionesPersistenceAdapter implements SolicitudVacacionesPort {

    private final SolicitudVacacionesJpaRepository repository;

    SolicitudVacacionesPersistenceAdapter(SolicitudVacacionesJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SolicitudVacaciones guardar(SolicitudVacaciones s) {
        repository
                .findById(s.id())
                .ifPresentOrElse(
                        existente -> existente.actualizarEstado(s.estado(), s.motivoRechazo()),
                        () -> repository.save(new SolicitudVacacionesEntity(
                                s.id(),
                                s.empleadoId(),
                                s.periodoId(),
                                s.desde(),
                                s.hasta(),
                                s.dias(),
                                s.estado(),
                                s.motivoRechazo())));
        repository.flush();
        return s;
    }

    @Override
    public Optional<SolicitudVacaciones> buscar(UUID id) {
        return repository.findById(id).map(SolicitudVacacionesPersistenceAdapter::aDominio);
    }

    @Override
    public List<SolicitudVacaciones> activasDelEmpleado(UUID empleadoId) {
        EnumSet<EstadoSolicitud> activas = EnumSet.noneOf(EstadoSolicitud.class);
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            if (estado.reservaDias()) {
                activas.add(estado);
            }
        }
        return aDominio(repository.findByEmpleadoIdAndEstadoIn(empleadoId, activas));
    }

    @Override
    public List<SolicitudVacaciones> listar(Collection<UUID> empleadoIds, EstadoSolicitud estado) {
        List<SolicitudVacacionesEntity> entidades;
        if (empleadoIds == null) {
            entidades = estado == null
                    ? repository.findAllByOrderByDesdeDesc()
                    : repository.findByEstadoOrderByDesdeDesc(estado);
        } else {
            entidades = estado == null
                    ? repository.findByEmpleadoIdInOrderByDesdeDesc(empleadoIds)
                    : repository.findByEmpleadoIdInAndEstadoOrderByDesdeDesc(empleadoIds, estado);
        }
        return aDominio(entidades);
    }

    @Override
    public List<SolicitudVacaciones> pendientesDeAprobacionQueSolapan(LocalDate desde, LocalDate hasta) {
        return aDominio(repository.findByEstadoInAndDesdeLessThanEqualAndHastaGreaterThanEqual(
                List.of(EstadoSolicitud.SOLICITADA, EstadoSolicitud.APROBADA_JEFE), hasta, desde));
    }

    @Override
    public List<SolicitudVacaciones> porIniciarGoce(LocalDate hoy) {
        return aDominio(repository.findByEstadoAndDesdeLessThanEqual(EstadoSolicitud.APROBADA_RRHH, hoy));
    }

    @Override
    public List<SolicitudVacaciones> porFinalizar(LocalDate hoy) {
        return aDominio(repository.findByEstadoAndHastaLessThan(EstadoSolicitud.EN_GOCE, hoy));
    }

    private static List<SolicitudVacaciones> aDominio(List<SolicitudVacacionesEntity> entidades) {
        return entidades.stream().map(SolicitudVacacionesPersistenceAdapter::aDominio).toList();
    }

    private static SolicitudVacaciones aDominio(SolicitudVacacionesEntity e) {
        return SolicitudVacaciones.reconstruir(
                e.getId(), e.getEmpleadoId(), e.getPeriodoId(), e.getDesde(), e.getHasta(), e.getEstado(),
                e.getMotivoRechazo());
    }
}
