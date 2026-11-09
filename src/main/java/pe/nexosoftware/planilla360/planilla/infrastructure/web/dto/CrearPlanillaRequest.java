package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** {@code periodo} va como {@code yyyy-MM}; si no se indica el tipo, es la planilla mensual. */
public record CrearPlanillaRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El periodo debe tener el formato yyyy-MM")
                String periodo,
        TipoPlanilla tipo) {
}
