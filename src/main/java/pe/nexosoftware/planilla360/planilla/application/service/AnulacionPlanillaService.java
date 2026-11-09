package pe.nexosoftware.planilla360.planilla.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.planilla.application.model.Actor;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.in.AnularPlanillaUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.PagoPlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;

/**
 * Anulación de una planilla. El dominio permite anular una planilla cerrada pero no conoce roles ni pagos: esas dos
 * condiciones (administrador de la empresa y sin pago registrado) las exige este caso de uso antes de invocarlo.
 */
@Service
@Transactional(readOnly = true)
public class AnulacionPlanillaService implements AnularPlanillaUseCase {

    private final PlanillaPort planillas;
    private final PagoPlanillaPort pagos;
    private final PublicadorEventosPort eventos;

    public AnulacionPlanillaService(PlanillaPort planillas, PagoPlanillaPort pagos, PublicadorEventosPort eventos) {
        this.planillas = planillas;
        this.pagos = pagos;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public Planilla anular(Actor actor, UUID planillaId, String motivo) {
        if (!actor.puedeAnularPlanillas()) {
            throw new AccesoProhibidoException("Solo el administrador de la empresa puede anular una planilla.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Indique el motivo de la anulación.");
        }
        Planilla planilla = planillas.buscarParaActualizar(planillaId).orElseThrow(PlanillaService::noExiste);
        if (pagos.buscar(planilla.id()).isPresent()) {
            throw new ConflictoException("No se puede anular la planilla: ya tiene un pago registrado.");
        }
        ResumenPlanilla antes = ResumenPlanilla.de(planilla);
        planilla.anular();
        Planilla anulada = planillas.guardar(planilla);

        Map<String, Object> despues = new LinkedHashMap<>();
        despues.put("planilla", ResumenPlanilla.de(anulada));
        despues.put("motivo", motivo.trim());
        despues.put("anuladaPor", actor.usuarioId());
        eventos.registrarCambio(anulada.id(), Accion.ACTUALIZAR, antes, despues);
        return anulada;
    }
}
