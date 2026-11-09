package pe.nexosoftware.planilla360.catalogo.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.catalogo.entity.Cargo;

public interface CargoRepository extends JpaRepository<Cargo, UUID> {

    Page<Cargo> findByAreaId(UUID areaId, Pageable pageable);

    java.util.Optional<Cargo> findByAreaIdAndNombreIgnoreCase(UUID areaId, String nombre);

    boolean existsByAreaId(UUID areaId);

    boolean existsByAreaIdAndNombreIgnoreCase(UUID areaId, String nombre);

    boolean existsByAreaIdAndNombreIgnoreCaseAndIdNot(UUID areaId, String nombre, UUID id);
}
