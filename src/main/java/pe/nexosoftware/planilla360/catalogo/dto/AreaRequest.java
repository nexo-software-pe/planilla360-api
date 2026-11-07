package pe.nexosoftware.planilla360.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AreaRequest(@NotBlank @Size(max = 100) String nombre) {
}
