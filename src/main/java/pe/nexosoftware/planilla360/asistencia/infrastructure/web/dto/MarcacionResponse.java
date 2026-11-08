package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import java.time.Instant;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

public record MarcacionResponse(TipoMarcacion tipo, Instant instante) {
}
