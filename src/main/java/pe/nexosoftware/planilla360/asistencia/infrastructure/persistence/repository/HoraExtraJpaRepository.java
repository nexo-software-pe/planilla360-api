package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.HoraExtraEntity;

public interface HoraExtraJpaRepository extends JpaRepository<HoraExtraEntity, UUID> {

    List<HoraExtraEntity> findAllByOrderByFechaDesc();

    List<HoraExtraEntity> findByEstadoOrderByFechaDesc(EstadoHoraExtra estado);

    List<HoraExtraEntity> findByEmpleadoIdInOrderByFechaDesc(Collection<UUID> empleadoIds);

    List<HoraExtraEntity> findByEmpleadoIdInAndEstadoOrderByFechaDesc(
            Collection<UUID> empleadoIds, EstadoHoraExtra estado);
}
