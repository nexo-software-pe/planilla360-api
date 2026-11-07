package pe.nexosoftware.planilla360.catalogo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.catalogo.entity.ParametroLegal;

public interface ParametroLegalRepository extends JpaRepository<ParametroLegal, UUID> {

    List<ParametroLegal> findAllByOrderByCodigoAscVigenteDesdeDesc();

    Optional<ParametroLegal> findByCodigoAndVigenteHastaIsNull(String codigo);
}
