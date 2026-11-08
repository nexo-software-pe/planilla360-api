package pe.nexosoftware.planilla360.empleado.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.empleado.entity.Contrato;

public interface ContratoRepository extends JpaRepository<Contrato, UUID> {

    List<Contrato> findByEmpleadoIdOrderByFechaInicioDesc(UUID empleadoId);
}
