package pe.nexosoftware.planilla360.empleado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Alta de un empleado con su primer contrato (la usa la importación masiva).
 *
 * @param tipoContrato INDEFINIDO, PLAZO_FIJO o PART_TIME
 * @param regimenPensionario ONP o AFP
 * @param tipoComision FLUJO o MIXTA (solo con AFP)
 */
public record NuevoEmpleado(
        String dni,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String telefono,
        LocalDate fechaIngreso,
        UUID areaId,
        UUID cargoId,
        String tipoContrato,
        LocalDate fechaFinContrato,
        BigDecimal sueldoBasico,
        int jornadaSemanal,
        String regimenPensionario,
        UUID afpId,
        String tipoComision) {
}
