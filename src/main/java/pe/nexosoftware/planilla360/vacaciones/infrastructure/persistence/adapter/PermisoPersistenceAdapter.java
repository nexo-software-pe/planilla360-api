package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PermisoPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.Permiso;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity.PermisoEntity;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.repository.PermisoJpaRepository;

@Component
class PermisoPersistenceAdapter implements PermisoPort {

    private final PermisoJpaRepository repository;

    PermisoPersistenceAdapter(PermisoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Permiso guardar(Permiso p) {
        repository
                .findById(p.id())
                .ifPresentOrElse(
                        existente -> existente.actualizarEstado(p.estado(), p.motivoRechazo()),
                        () -> repository.save(new PermisoEntity(
                                p.id(),
                                p.empleadoId(),
                                p.tipo(),
                                p.desde(),
                                p.hasta(),
                                p.conGoce(),
                                p.motivo(),
                                p.estado(),
                                p.motivoRechazo())));
        repository.flush();
        return p;
    }

    @Override
    public Optional<Permiso> buscar(UUID id) {
        return repository.findById(id).map(PermisoPersistenceAdapter::aDominio);
    }

    @Override
    public List<Permiso> aprobadosSinGoceQueSolapan(LocalDate desde, LocalDate hasta) {
        return repository
                .findByEstadoAndConGoceFalseAndDesdeLessThanEqualAndHastaGreaterThanEqual(
                        EstadoPermiso.APROBADO, hasta, desde)
                .stream()
                .map(PermisoPersistenceAdapter::aDominio)
                .toList();
    }

    @Override
    public List<Permiso> listar(Collection<UUID> empleadoIds, EstadoPermiso estado) {
        List<PermisoEntity> entidades;
        if (empleadoIds == null) {
            entidades = estado == null
                    ? repository.findAllByOrderByDesdeDesc()
                    : repository.findByEstadoOrderByDesdeDesc(estado);
        } else {
            entidades = estado == null
                    ? repository.findByEmpleadoIdInOrderByDesdeDesc(empleadoIds)
                    : repository.findByEmpleadoIdInAndEstadoOrderByDesdeDesc(empleadoIds, estado);
        }
        return entidades.stream().map(PermisoPersistenceAdapter::aDominio).toList();
    }

    private static Permiso aDominio(PermisoEntity e) {
        return Permiso.reconstruir(
                e.getId(), e.getEmpleadoId(), e.getTipo(), e.getDesde(), e.getHasta(), e.isConGoce(), e.getMotivo(),
                e.getEstado(), e.getMotivoRechazo());
    }
}
