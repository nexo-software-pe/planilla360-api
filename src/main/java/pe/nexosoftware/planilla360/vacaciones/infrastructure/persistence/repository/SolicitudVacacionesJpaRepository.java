package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity.SolicitudVacacionesEntity;

public interface SolicitudVacacionesJpaRepository extends JpaRepository<SolicitudVacacionesEntity, UUID> {

    List<SolicitudVacacionesEntity> findByEmpleadoIdAndEstadoIn(UUID empleadoId, Collection<EstadoSolicitud> estados);

    List<SolicitudVacacionesEntity> findAllByOrderByDesdeDesc();

    List<SolicitudVacacionesEntity> findByEstadoOrderByDesdeDesc(EstadoSolicitud estado);

    List<SolicitudVacacionesEntity> findByEmpleadoIdInOrderByDesdeDesc(Collection<UUID> empleadoIds);

    List<SolicitudVacacionesEntity> findByEmpleadoIdInAndEstadoOrderByDesdeDesc(
            Collection<UUID> empleadoIds, EstadoSolicitud estado);

    List<SolicitudVacacionesEntity> findByEstadoAndDesdeLessThanEqual(EstadoSolicitud estado, LocalDate fecha);

    List<SolicitudVacacionesEntity> findByEstadoInAndDesdeLessThanEqualAndHastaGreaterThanEqual(
            Collection<EstadoSolicitud> estados, LocalDate hasta, LocalDate desde);

    List<SolicitudVacacionesEntity> findByEstadoAndHastaLessThan(EstadoSolicitud estado, LocalDate fecha);
}
