package pe.nexosoftware.planilla360.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CargoRequest(
        @NotNull UUID areaId,
        @NotBlank @Size(max = 100) String nombre,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal sueldoReferencial) {
}
