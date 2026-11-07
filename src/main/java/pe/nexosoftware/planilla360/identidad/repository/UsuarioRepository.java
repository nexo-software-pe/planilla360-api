package pe.nexosoftware.planilla360.identidad.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<Usuario> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Usuario> findByIdAndTenantId(UUID id, UUID tenantId);
}
