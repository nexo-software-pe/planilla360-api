package pe.nexosoftware.planilla360.empleado;

import java.time.LocalDate;
import java.util.UUID;

/** Datos de identificación de un empleado para documentos y avisos (boletas, correos). */
public record FichaEmpleado(
        UUID id,
        String dni,
        String nombreCompleto,
        String email,
        String area,
        String cargo,
        LocalDate fechaIngreso) {
}
