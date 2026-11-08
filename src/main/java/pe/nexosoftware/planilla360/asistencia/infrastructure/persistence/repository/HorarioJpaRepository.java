package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.HorarioEntity;

public interface HorarioJpaRepository extends JpaRepository<HorarioEntity, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<HorarioEntity> findAllByOrderByNombreAsc();
}
