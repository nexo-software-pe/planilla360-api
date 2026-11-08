package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity.PermisoEntity;

public interface PermisoJpaRepository extends JpaRepository<PermisoEntity, UUID> {

    List<PermisoEntity> findByEstadoAndConGoceFalseAndDesdeLessThanEqualAndHastaGreaterThanEqual(
            EstadoPermiso estado, LocalDate hasta, LocalDate desde);

    List<PermisoEntity> findAllByOrderByDesdeDesc();

    List<PermisoEntity> findByEstadoOrderByDesdeDesc(EstadoPermiso estado);

    List<PermisoEntity> findByEmpleadoIdInOrderByDesdeDesc(Collection<UUID> empleadoIds);

    List<PermisoEntity> findByEmpleadoIdInAndEstadoOrderByDesdeDesc(
            Collection<UUID> empleadoIds, EstadoPermiso estado);
}
