package pe.nexosoftware.planilla360.planilla.application.model;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Planilla sin sus líneas: lo que muestran los listados y el encabezado del detalle. */
public record ResumenPlanilla(
        UUID id,
        Periodo periodo,
        TipoPlanilla tipo,
        EstadoPlanilla estado,
        int trabajadores,
        Dinero totalIngresos,
        Dinero totalDescuentos,
        Dinero totalNeto,
        Dinero totalEssalud,
        Instant cerradaEn) {

    public static ResumenPlanilla de(Planilla planilla) {
        return new ResumenPlanilla(
                planilla.id(),
                planilla.periodo(),
                planilla.tipo(),
                planilla.estado(),
                planilla.lineas().size(),
                planilla.totalIngresos(),
                planilla.totalDescuentos(),
                planilla.totalNeto(),
                planilla.totalEssalud(),
                planilla.cerradaEn().orElse(null));
    }
}
