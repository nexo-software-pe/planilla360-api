package pe.nexosoftware.planilla360.empleado.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.empleado.model.TipoComision;
import pe.nexosoftware.planilla360.empleado.model.TipoContrato;
import pe.nexosoftware.planilla360.empleado.model.TipoRegimenPensionario;

public record ContratoResponse(
        UUID id,
        UUID empleadoId,
        TipoContrato tipo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal sueldoBasico,
        int jornadaSemanal,
        int diasLaborables,
        TipoRegimenPensionario regimenPensionario,
        UUID afpId,
        TipoComision tipoComision,
        String cuspp) {
}
