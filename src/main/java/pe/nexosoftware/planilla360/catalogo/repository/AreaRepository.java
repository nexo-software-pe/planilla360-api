package pe.nexosoftware.planilla360.catalogo.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.catalogo.entity.Area;

/** Las consultas se filtran automáticamente por la empresa del contexto (Hibernate {@code @TenantId}). */
public interface AreaRepository extends JpaRepository<Area, UUID> {

    java.util.Optional<Area> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
