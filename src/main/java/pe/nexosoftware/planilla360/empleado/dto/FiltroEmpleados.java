package pe.nexosoftware.planilla360.empleado.dto;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.empleado.model.EstadoEmpleado;

/** Criterios opcionales de búsqueda de empleados; los que vienen nulos no restringen. */
public record FiltroEmpleados(
        String q,
        UUID areaId,
        UUID cargoId,
        UUID jefeId,
        EstadoEmpleado estado,
        LocalDate ingresoDesde,
        LocalDate ingresoHasta) {
}
