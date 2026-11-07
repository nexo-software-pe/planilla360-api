package pe.nexosoftware.planilla360.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParametroLegalRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,60}$", message = "El código solo admite mayúsculas, números y _")
                String codigo,
        @NotBlank @Size(max = 200) String descripcion,
        @NotNull @DecimalMin("0.0000") BigDecimal valor,
        @NotNull LocalDate vigenteDesde) {
}
