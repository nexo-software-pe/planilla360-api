package pe.nexosoftware.planilla360.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.nexosoftware.planilla360.compartido.validacion.RucValido;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;

public record EmpresaRequest(
        @NotBlank @RucValido String ruc,
        @NotBlank @Size(max = 200) String razonSocial,
        @NotNull RegimenLaboral regimenLaboral,
        @NotNull PlanSaas plan) {
}
