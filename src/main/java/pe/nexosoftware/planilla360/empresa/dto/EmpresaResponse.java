package pe.nexosoftware.planilla360.empresa.dto;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;

public record EmpresaResponse(
        UUID id,
        String ruc,
        String razonSocial,
        RegimenLaboral regimenLaboral,
        PlanSaas plan,
        boolean activa,
        Instant creadaEn) {
}
