package pe.nexosoftware.planilla360.boleta.dto;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.boleta.model.EstadoBoleta;

public record BoletaResponse(
        UUID id,
        UUID planillaId,
        UUID empleadoId,
        String periodo,
        String tipo,
        EstadoBoleta estado,
        Instant generadaEn,
        Instant enviadaEn) {
}
