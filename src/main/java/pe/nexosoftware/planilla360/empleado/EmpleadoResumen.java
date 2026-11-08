package pe.nexosoftware.planilla360.empleado;

import java.time.LocalDate;
import java.util.UUID;

/** Vista pública mínima de un empleado para el resto de módulos. */
public record EmpleadoResumen(
        UUID id, String nombreCompleto, UUID areaId, UUID jefeId, LocalDate fechaIngreso, LocalDate fechaCese) {

    /** Un empleado está vigente en una fecha si ya ingresó y no había cesado. */
    public boolean vigenteEn(LocalDate fecha) {
        return !fechaIngreso.isAfter(fecha) && (fechaCese == null || !fechaCese.isBefore(fecha));
    }
}
