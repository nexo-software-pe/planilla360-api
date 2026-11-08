package pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record ResolverHoraExtraRequest(@NotNull Accion accion) {

    public enum Accion {
        APROBAR,
        RECHAZAR
    }
}
