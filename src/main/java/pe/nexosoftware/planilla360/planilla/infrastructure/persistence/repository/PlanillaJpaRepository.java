package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.PlanillaEntity;

/** Las consultas se filtran solas por la empresa del contexto (Hibernate {@code @TenantId}). */
public interface PlanillaJpaRepository
        extends JpaRepository<PlanillaEntity, UUID>, JpaSpecificationExecutor<PlanillaEntity> {

    /** Bloquea la fila hasta el fin de la transacción: serializa cálculo, cierre, anulación y pago. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanillaEntity p where p.id = :id")
    Optional<PlanillaEntity> buscarConBloqueo(@Param("id") UUID id);

    Optional<PlanillaEntity> findByIdempotencyKey(String clave);

    boolean existsByPeriodoAndTipoAndEstadoIn(String periodo, TipoPlanilla tipo, Collection<EstadoPlanilla> estados);

    boolean existsByPeriodoAndTipoAndEstadoAndIdNot(
            String periodo, TipoPlanilla tipo, EstadoPlanilla estado, UUID id);
}
