package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.AsistenciaDiariaEntity;

public interface AsistenciaDiariaJpaRepository extends JpaRepository<AsistenciaDiariaEntity, UUID> {

    Optional<AsistenciaDiariaEntity> findByEmpleadoIdAndFecha(UUID empleadoId, LocalDate fecha);

    List<AsistenciaDiariaEntity> findByEmpleadoIdAndFechaBetweenOrderByFecha(
            UUID empleadoId, LocalDate desde, LocalDate hasta);

    List<AsistenciaDiariaEntity> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
