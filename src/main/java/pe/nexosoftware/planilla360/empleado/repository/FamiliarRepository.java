package pe.nexosoftware.planilla360.empleado.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.empleado.entity.Familiar;

public interface FamiliarRepository extends JpaRepository<Familiar, UUID> {

    List<Familiar> findByEmpleadoIdOrderByFechaNacimientoAsc(UUID empleadoId);
}
