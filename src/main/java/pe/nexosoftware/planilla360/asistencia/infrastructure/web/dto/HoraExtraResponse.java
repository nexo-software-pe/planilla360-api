package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;

public record HoraExtraResponse(
        UUID id,
        UUID empleadoId,
        LocalDate fecha,
        BigDecimal horas,
        String motivo,
        EstadoHoraExtra estado,
        UUID aprobadoPor) {
}
