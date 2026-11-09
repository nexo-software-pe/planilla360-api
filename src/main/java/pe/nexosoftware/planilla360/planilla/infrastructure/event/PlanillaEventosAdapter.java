package pe.nexosoftware.planilla360.planilla.infrastructure.event;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.planilla.PlanillaCerradaEvento;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.event.PlanillaCerrada;

/** Publica con los eventos de Spring (síncronos, en la misma transacción del cierre). */
@Component
class PlanillaEventosAdapter implements PublicadorEventosPort {

    private static final String ENTIDAD = "PLANILLA";

    private final ApplicationEventPublisher publicador;

    PlanillaEventosAdapter(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(PlanillaCerrada evento) {
        publicador.publishEvent(new PlanillaCerradaEvento(
                evento.planillaId(),
                evento.empresaId(),
                evento.periodo().valor(),
                evento.tipo().name(),
                evento.cerradaEn(),
                evento.trabajadores(),
                evento.totalNeto().monto()));
    }

    @Override
    public void registrarCambio(UUID planillaId, Accion accion, Object antes, Object despues) {
        publicador.publishEvent(new CambioRegistrado(
                ENTIDAD, planillaId, TenantContext.requerido(), accion, antes, despues));
    }
}
