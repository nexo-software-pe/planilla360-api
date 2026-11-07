package pe.nexosoftware.planilla360.empresa.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    boolean existsByRuc(String ruc);

    Page<Empresa> findByActiva(boolean activa, Pageable pageable);
}
