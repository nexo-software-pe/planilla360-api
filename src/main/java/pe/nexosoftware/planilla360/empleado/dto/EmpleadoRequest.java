package pe.nexosoftware.planilla360.empleado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record EmpleadoRequest(
        @NotBlank @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos") String dni,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotNull @Past LocalDate fechaNacimiento,
        @Email @Size(max = 160) String email,
        @Size(max = 20) String telefono,
        @NotNull LocalDate fechaIngreso,
        @NotNull UUID areaId,
        @NotNull UUID cargoId,
        UUID jefeId) {
}
