package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity.PeriodoVacacionalEntity;

public interface PeriodoVacacionalJpaRepository extends JpaRepository<PeriodoVacacionalEntity, UUID> {

    List<PeriodoVacacionalEntity> findByEmpleadoIdOrderByDesde(UUID empleadoId);
}
