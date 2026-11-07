package pe.nexosoftware.planilla360.planilla.domain.model;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Renglón de la boleta: código estable, texto para el trabajador, tipo y monto. */
public record Concepto(String codigo, String descripcion, TipoConcepto tipo, Dinero monto) {
}
