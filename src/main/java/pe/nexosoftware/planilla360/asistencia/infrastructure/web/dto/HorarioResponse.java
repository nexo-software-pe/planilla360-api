package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record HorarioResponse(
        UUID id, String nombre, LocalTime entrada, LocalTime salida, int toleranciaMin, Set<DayOfWeek> dias,
        int refrigerioMin) {
}
