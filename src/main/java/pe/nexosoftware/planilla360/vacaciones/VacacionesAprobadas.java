package pe.nexosoftware.planilla360.vacaciones;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de dominio: RRHH dio la aprobación final a unas vacaciones. Asistencia lo escucha para marcar
 * esos días como justificados.
 */
public record VacacionesAprobadas(
        UUID solicitudId, UUID empleadoId, UUID tenantId, LocalDate desde, LocalDate hasta, int dias) {
}
