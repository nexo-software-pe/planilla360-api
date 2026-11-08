package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.PagoPlanillaEntity;

public interface PagoPlanillaJpaRepository extends JpaRepository<PagoPlanillaEntity, UUID> {

    Optional<PagoPlanillaEntity> findByPlanillaId(UUID planillaId);
}
