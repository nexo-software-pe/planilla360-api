package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PeriodoVacacionalPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PeriodoVacacional;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity.PeriodoVacacionalEntity;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.repository.PeriodoVacacionalJpaRepository;

@Component
class PeriodoVacacionalPersistenceAdapter implements PeriodoVacacionalPort {

    private final PeriodoVacacionalJpaRepository repository;

    PeriodoVacacionalPersistenceAdapter(PeriodoVacacionalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PeriodoVacacional> delEmpleado(UUID empleadoId) {
        return repository.findByEmpleadoIdOrderByDesde(empleadoId).stream()
                .map(PeriodoVacacionalPersistenceAdapter::aDominio)
                .toList();
    }

    @Override
    public Optional<Guardado> buscar(UUID periodoId) {
        return repository.findById(periodoId)
                .map(e -> new Guardado(e.getEmpleadoId(), aDominio(e)));
    }

    @Override
    public void guardar(UUID empleadoId, PeriodoVacacional periodo) {
        repository
                .findById(periodo.id())
                .ifPresentOrElse(
                        existente -> existente.actualizarDias(periodo.diasGozados(), periodo.diasVendidos()),
                        () -> repository.save(new PeriodoVacacionalEntity(
                                periodo.id(),
                                empleadoId,
                                periodo.desde(),
                                periodo.hasta(),
                                periodo.diasGanados(),
                                periodo.diasGozados(),
                                periodo.diasVendidos())));
        repository.flush();
    }

    private static PeriodoVacacional aDominio(PeriodoVacacionalEntity e) {
        return new PeriodoVacacional(
                e.getId(), e.getDesde(), e.getHasta(), e.getDiasGanados(), e.getDiasGozados(), e.getDiasVendidos());
    }
}
