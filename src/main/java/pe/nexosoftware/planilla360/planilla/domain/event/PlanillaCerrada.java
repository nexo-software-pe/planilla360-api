package pe.nexosoftware.planilla360.planilla.domain.event;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Evento de dominio: una planilla quedó cerrada. Dispara la generación de boletas y los correos. */
public record PlanillaCerrada(
        UUID planillaId,
        UUID empresaId,
        Periodo periodo,
        TipoPlanilla tipo,
        Instant cerradaEn,
        int trabajadores,
        Dinero totalNeto) {
}
