package pe.nexosoftware.planilla360.planilla.application.model;

import java.time.LocalDate;
import java.util.UUID;

/** Pago registrado de una planilla cerrada. */
public record PagoPlanilla(UUID planillaId, LocalDate fechaPago, String referencia) {
}
