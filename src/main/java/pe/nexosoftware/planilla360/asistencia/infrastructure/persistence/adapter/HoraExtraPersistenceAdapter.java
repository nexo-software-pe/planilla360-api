package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HoraExtraPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import pe.nexosoftware.planilla360.asistencia.domain.model.HoraExtra;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.HoraExtraEntity;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository.HoraExtraJpaRepository;

@Component
class HoraExtraPersistenceAdapter implements HoraExtraPort {

    private final HoraExtraJpaRepository repository;

    HoraExtraPersistenceAdapter(HoraExtraJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public HoraExtra guardar(HoraExtra horaExtra) {
        repository
                .findById(horaExtra.id())
                .ifPresentOrElse(
                        existente -> existente.resolverCon(horaExtra.estado(), horaExtra.aprobadoPor()),
                        () -> repository.save(new HoraExtraEntity(
                                horaExtra.id(),
                                horaExtra.empleadoId(),
                                horaExtra.fecha(),
                                horaExtra.horas(),
                                horaExtra.motivo(),
                                horaExtra.estado(),
                                horaExtra.aprobadoPor())));
        repository.flush();
        return horaExtra;
    }

    @Override
    public Optional<HoraExtra> buscar(UUID id) {
        return repository.findById(id).map(HoraExtraPersistenceAdapter::aDominio);
    }

    @Override
    public List<HoraExtra> aprobadasEntre(LocalDate desde, LocalDate hasta) {
        return repository.findByEstadoAndFechaBetween(EstadoHoraExtra.APROBADA, desde, hasta).stream()
                .map(HoraExtraPersistenceAdapter::aDominio)
                .toList();
    }

    @Override
    public List<HoraExtra> listar(Collection<UUID> empleadoIds, EstadoHoraExtra estado) {
        List<HoraExtraEntity> entidades;
        if (empleadoIds == null) {
            entidades = estado == null
                    ? repository.findAllByOrderByFechaDesc()
                    : repository.findByEstadoOrderByFechaDesc(estado);
        } else {
            entidades = estado == null
                    ? repository.findByEmpleadoIdInOrderByFechaDesc(empleadoIds)
                    : repository.findByEmpleadoIdInAndEstadoOrderByFechaDesc(empleadoIds, estado);
        }
        return entidades.stream().map(HoraExtraPersistenceAdapter::aDominio).toList();
    }

    private static HoraExtra aDominio(HoraExtraEntity e) {
        return HoraExtra.reconstruir(
                e.getId(), e.getEmpleadoId(), e.getFecha(), e.getHoras(), e.getMotivo(), e.getEstado(), e.getAprobadoPor());
    }
}
