package pe.nexosoftware.planilla360.empleado.dto;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.empleado.model.Parentesco;

public record FamiliarResponse(
        UUID id, UUID empleadoId, Parentesco parentesco, String nombres, LocalDate fechaNacimiento, boolean estudia) {
}
