package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.out.MarcacionPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.MarcacionEntity;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository.MarcacionJpaRepository;

@Component
class MarcacionPersistenceAdapter implements MarcacionPort {

    private final MarcacionJpaRepository repository;

    MarcacionPersistenceAdapter(MarcacionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void guardar(UUID empleadoId, Marcacion marcacion) {
        repository.saveAndFlush(new MarcacionEntity(empleadoId, marcacion.instante(), marcacion.tipo()));
    }

    @Override
    public List<Marcacion> entre(UUID empleadoId, Instant desde, Instant hasta) {
        return repository
                .findByEmpleadoIdAndInstanteGreaterThanEqualAndInstanteLessThanOrderByInstante(empleadoId, desde, hasta)
                .stream()
                .map(MarcacionPersistenceAdapter::aDominio)
                .toList();
    }

    @Override
    public Map<UUID, List<Marcacion>> entreDeTodos(Instant desde, Instant hasta) {
        Map<UUID, List<Marcacion>> porEmpleado = new HashMap<>();
        for (MarcacionEntity entidad : repository.findByInstanteGreaterThanEqualAndInstanteLessThan(desde, hasta)) {
            porEmpleado
                    .computeIfAbsent(entidad.getEmpleadoId(), id -> new java.util.ArrayList<>())
                    .add(aDominio(entidad));
        }
        return porEmpleado;
    }

    private static Marcacion aDominio(MarcacionEntity entidad) {
        return new Marcacion(entidad.getInstante(), entidad.getTipo());
    }
}
