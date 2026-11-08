package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.DiaJustificadoEntity;

public interface DiaJustificadoJpaRepository extends JpaRepository<DiaJustificadoEntity, UUID> {

    List<DiaJustificadoEntity> findByFecha(LocalDate fecha);

    List<DiaJustificadoEntity> findByEmpleadoIdAndFechaIn(UUID empleadoId, Collection<LocalDate> fechas);
}
