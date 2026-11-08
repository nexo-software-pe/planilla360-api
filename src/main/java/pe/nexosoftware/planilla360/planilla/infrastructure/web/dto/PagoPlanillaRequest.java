package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PagoPlanillaRequest(@NotNull LocalDate fechaPago, @NotBlank @Size(max = 80) String referencia) {
}
