package pe.nexosoftware.planilla360.empresa.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoEmpresaRequest(@NotNull Boolean activa) {
}
