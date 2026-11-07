package pe.nexosoftware.planilla360.identidad.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(@NotNull Boolean activo) {
}
