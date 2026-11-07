package pe.nexosoftware.planilla360.catalogo.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.catalogo.entity.Afp;

public interface AfpRepository extends JpaRepository<Afp, UUID> {

    List<Afp> findAllByOrderByNombreAsc();
}
