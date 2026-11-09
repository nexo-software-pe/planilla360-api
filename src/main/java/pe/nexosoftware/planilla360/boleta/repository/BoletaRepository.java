package pe.nexosoftware.planilla360.boleta.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.nexosoftware.planilla360.boleta.entity.Boleta;
import pe.nexosoftware.planilla360.boleta.model.EstadoBoleta;

/** Las consultas se filtran automáticamente por la empresa del contexto (Hibernate {@code @TenantId}). */
public interface BoletaRepository extends JpaRepository<Boleta, UUID> {

    Optional<Boleta> findByPlanillaIdAndEmpleadoId(UUID planillaId, UUID empleadoId);

    List<Boleta> findByPlanillaId(UUID planillaId);

    Page<Boleta> findByPlanillaId(UUID planillaId, Pageable pageable);

    Page<Boleta> findByEmpleadoIdOrderByCreatedAtDesc(UUID empleadoId, Pageable pageable);

    @Query("select b.estado, count(b) from Boleta b where b.planillaId = :planillaId group by b.estado")
    List<Object[]> contarPorEstado(UUID planillaId);

    List<Boleta> findByPlanillaIdAndEstado(UUID planillaId, EstadoBoleta estado);
}
