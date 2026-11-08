package pe.nexosoftware.planilla360.empleado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import pe.nexosoftware.planilla360.empleado.model.Parentesco;

public record FamiliarRequest(
        @NotNull Parentesco parentesco,
        @NotBlank @Size(max = 150) String nombres,
        @NotNull @PastOrPresent LocalDate fechaNacimiento,
        boolean estudia) {
}
