package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.MarcacionEntity;

public interface MarcacionJpaRepository extends JpaRepository<MarcacionEntity, UUID> {

    List<MarcacionEntity> findByEmpleadoIdAndInstanteGreaterThanEqualAndInstanteLessThanOrderByInstante(
            UUID empleadoId, Instant desde, Instant hasta);

    List<MarcacionEntity> findByInstanteGreaterThanEqualAndInstanteLessThan(Instant desde, Instant hasta);
}
