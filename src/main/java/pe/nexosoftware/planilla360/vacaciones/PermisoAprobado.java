package pe.nexosoftware.planilla360.vacaciones;

import java.time.LocalDate;
import java.util.UUID;

/** Evento de dominio: se aprobó un permiso o licencia. Los días quedan justificados en asistencia. */
public record PermisoAprobado(
        UUID permisoId, UUID empleadoId, UUID tenantId, LocalDate desde, LocalDate hasta, boolean conGoce) {
}
