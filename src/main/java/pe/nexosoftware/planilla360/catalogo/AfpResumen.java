package pe.nexosoftware.planilla360.catalogo;

import java.math.BigDecimal;
import java.util.UUID;

/** Vista pública de una AFP y sus comisiones (porcentajes). */
public record AfpResumen(UUID id, String nombre, BigDecimal comisionFlujo, BigDecimal comisionMixta) {
}
