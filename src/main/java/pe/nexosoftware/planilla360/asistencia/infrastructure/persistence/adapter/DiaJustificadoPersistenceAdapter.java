package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.in.JustificarDiasUseCase.Motivo;
import pe.nexosoftware.planilla360.asistencia.application.port.out.DiaJustificadoPort;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.DiaJustificadoEntity;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository.DiaJustificadoJpaRepository;

@Component
class DiaJustificadoPersistenceAdapter implements DiaJustificadoPort {

    private final DiaJustificadoJpaRepository repository;

    DiaJustificadoPersistenceAdapter(DiaJustificadoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void justificar(UUID empleadoId, List<LocalDate> fechas, Motivo motivo, UUID origenId) {
        Set<LocalDate> yaJustificadas = new HashSet<>();
        repository.findByEmpleadoIdAndFechaIn(empleadoId, fechas).forEach(d -> yaJustificadas.add(d.getFecha()));
        List<DiaJustificadoEntity> nuevas = fechas.stream()
                .filter(f -> !yaJustificadas.contains(f))
                .map(f -> new DiaJustificadoEntity(empleadoId, f, motivo, origenId))
                .toList();
        repository.saveAllAndFlush(nuevas);
    }

    @Override
    public Set<UUID> empleadosJustificadosEn(LocalDate fecha) {
        return repository.findByFecha(fecha).stream()
                .map(DiaJustificadoEntity::getEmpleadoId)
                .collect(Collectors.toSet());
    }
}
