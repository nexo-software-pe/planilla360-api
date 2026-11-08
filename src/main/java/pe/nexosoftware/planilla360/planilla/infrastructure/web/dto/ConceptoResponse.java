package pe.nexosoftware.planilla360.planilla.infrastructure.web.dto;

import java.math.BigDecimal;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoConcepto;

public record ConceptoResponse(String codigo, String descripcion, TipoConcepto tipo, BigDecimal monto) {
}
