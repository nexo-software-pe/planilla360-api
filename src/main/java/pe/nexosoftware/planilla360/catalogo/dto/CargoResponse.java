package pe.nexosoftware.planilla360.catalogo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CargoResponse(UUID id, UUID areaId, String nombre, BigDecimal sueldoReferencial) {
}
