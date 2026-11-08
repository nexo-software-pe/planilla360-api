package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.EmpleadoHorarioEntity;

public interface EmpleadoHorarioJpaRepository extends JpaRepository<EmpleadoHorarioEntity, UUID> {

    Optional<EmpleadoHorarioEntity> findByEmpleadoIdAndHastaIsNull(UUID empleadoId);

    @Query("select a from EmpleadoHorarioEntity a where a.desde <= :fecha and (a.hasta is null or a.hasta >= :fecha)")
    List<EmpleadoHorarioEntity> findVigentesEn(@Param("fecha") LocalDate fecha);
}
