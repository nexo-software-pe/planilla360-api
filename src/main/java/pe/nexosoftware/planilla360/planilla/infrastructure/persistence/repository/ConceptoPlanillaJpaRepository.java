package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.ConceptoPlanillaEntity;

public interface ConceptoPlanillaJpaRepository extends JpaRepository<ConceptoPlanillaEntity, UUID> {
}
