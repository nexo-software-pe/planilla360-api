package pe.nexosoftware.planilla360.empleado.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.empleado.model.TipoComision;
import pe.nexosoftware.planilla360.empleado.model.TipoContrato;
import pe.nexosoftware.planilla360.empleado.model.TipoRegimenPensionario;

public record ContratoRequest(
        @NotNull TipoContrato tipo,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal sueldoBasico,
        @NotNull @Min(1) @Max(48) Integer jornadaSemanal,
        @Min(1) @Max(7) Integer diasLaborables,
        @NotNull TipoRegimenPensionario regimenPensionario,
        UUID afpId,
        TipoComision tipoComision,
        @Size(max = 12) String cuspp) {
}
