package pe.nexosoftware.planilla360.planilla.application.port.in;

import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.application.model.Actor;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;

public interface AnularPlanillaUseCase {

    /** Anula una planilla. Solo el administrador de la empresa y mientras no haya un pago registrado. */
    Planilla anular(Actor actor, UUID planillaId, String motivo);
}
