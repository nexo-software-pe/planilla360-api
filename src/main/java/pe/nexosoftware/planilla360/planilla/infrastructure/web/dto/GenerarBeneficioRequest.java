package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Si {@code simular} no se indica, solo se simula: no se guarda nada. */
public record GenerarBeneficioRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El periodo debe tener el formato yyyy-MM")
                String periodo,
        Boolean simular) {

    public boolean soloSimular() {
        return simular == null || simular;
    }
}
