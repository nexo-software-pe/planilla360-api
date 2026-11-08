package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.out.AsistenciaDiariaPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.AsistenciaDiariaEntity;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository.AsistenciaDiariaJpaRepository;

@Component
class AsistenciaDiariaPersistenceAdapter implements AsistenciaDiariaPort {

    private final AsistenciaDiariaJpaRepository repository;

    AsistenciaDiariaPersistenceAdapter(AsistenciaDiariaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void reemplazar(UUID empleadoId, AsistenciaDiaria dia) {
        repository
                .findByEmpleadoIdAndFecha(empleadoId, dia.fecha())
                .ifPresentOrElse(
                        existente -> existente.reemplazarCon(dia.minutosTardanza(), dia.minutosExtra(), dia.estado()),
                        () -> repository.save(new AsistenciaDiariaEntity(
                                empleadoId, dia.fecha(), dia.minutosTardanza(), dia.minutosExtra(), dia.estado())));
    }

    @Override
    public List<AsistenciaDiaria> delEmpleado(UUID empleadoId, LocalDate desde, LocalDate hasta) {
        return repository.findByEmpleadoIdAndFechaBetweenOrderByFecha(empleadoId, desde, hasta).stream()
                .map(AsistenciaDiariaPersistenceAdapter::aDominio)
                .toList();
    }

    @Override
    public List<Registro> delPeriodo(LocalDate desde, LocalDate hasta) {
        return repository.findByFechaBetween(desde, hasta).stream()
                .map(e -> new Registro(e.getEmpleadoId(), aDominio(e)))
                .toList();
    }

    private static AsistenciaDiaria aDominio(AsistenciaDiariaEntity e) {
        return new AsistenciaDiaria(e.getFecha(), e.getMinutosTardanza(), e.getMinutosExtra(), e.getEstado());
    }
}
