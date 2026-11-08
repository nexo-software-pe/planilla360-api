package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PagoPlanillaResponse(UUID planillaId, LocalDate fechaPago, String referencia) {
}
