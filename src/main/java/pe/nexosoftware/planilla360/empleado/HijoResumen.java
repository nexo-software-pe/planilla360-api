package pe.nexosoftware.planilla360.empleado;

import java.time.LocalDate;

/** Vista pública de un hijo del empleado: lo único que importa para la asignación familiar. */
public record HijoResumen(LocalDate fechaNacimiento, boolean estudia) {
}
