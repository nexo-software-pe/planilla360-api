package pe.nexosoftware.planilla360.catalogo.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ParametroLegalResponse(
        UUID id,
        String codigo,
        String descripcion,
        BigDecimal valor,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta) implements Serializable {
}
