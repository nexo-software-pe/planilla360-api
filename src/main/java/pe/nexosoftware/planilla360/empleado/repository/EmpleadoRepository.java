package pe.nexosoftware.planilla360.empleado.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.nexosoftware.planilla360.empleado.entity.Empleado;

/** Las consultas se filtran automáticamente por la empresa del contexto (Hibernate {@code @TenantId}). */
public interface EmpleadoRepository extends JpaRepository<Empleado, UUID>, JpaSpecificationExecutor<Empleado> {

    boolean existsByDni(String dni);

    @Query("select e from Empleado e where e.fechaIngreso <= :fecha "
            + "and (e.fechaCese is null or e.fechaCese >= :fecha) order by e.apellidos, e.nombres")
    List<Empleado> findVigentesEn(@Param("fecha") LocalDate fecha);

    @Query("select e from Empleado e where e.fechaIngreso <= :hasta "
            + "and (e.fechaCese is null or e.fechaCese >= :desde) order by e.apellidos, e.nombres")
    List<Empleado> findVigentesEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
