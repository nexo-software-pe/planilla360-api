package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;

public interface MarcacionPort {

    void guardar(UUID empleadoId, Marcacion marcacion);

    /** Marcaciones del empleado en el intervalo {@code [desde, hasta)}. */
    List<Marcacion> entre(UUID empleadoId, Instant desde, Instant hasta);

    /** Marcaciones de todos los empleados de la empresa en el intervalo {@code [desde, hasta)}. */
    Map<UUID, List<Marcacion>> entreDeTodos(Instant desde, Instant hasta);
}
