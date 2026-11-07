package pe.nexosoftware.planilla360.catalogo.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AfpResponse(
        UUID id, String nombre, BigDecimal comisionFlujo, BigDecimal comisionMixta, LocalDate vigenteDesde)
        implements Serializable {
}
