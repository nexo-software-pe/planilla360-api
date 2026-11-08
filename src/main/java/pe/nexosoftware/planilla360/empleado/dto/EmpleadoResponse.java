package pe.nexosoftware.planilla360.empleado.dto;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.empleado.model.EstadoEmpleado;

public record EmpleadoResponse(
        UUID id,
        String dni,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String telefono,
        LocalDate fechaIngreso,
        LocalDate fechaCese,
        UUID areaId,
        UUID cargoId,
        UUID jefeId,
        EstadoEmpleado estado) {
}
