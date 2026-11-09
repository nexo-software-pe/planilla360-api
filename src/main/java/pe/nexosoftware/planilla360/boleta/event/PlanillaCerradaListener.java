package pe.nexosoftware.planilla360.boleta.event;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.nexosoftware.planilla360.boleta.service.BoletaService;
import pe.nexosoftware.planilla360.compartido.eventos.CargaHistorica;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.planilla.PlanillaCerradaEvento;

/**
 * Al cerrarse una planilla (ya confirmada la transacción del cierre) dispara la generación masiva de boletas. Un
 * fallo aquí no revierte el cierre: se registra y RRHH puede relanzar la generación desde el endpoint.
 */
@Component
class PlanillaCerradaListener {

    private static final Logger LOG = LoggerFactory.getLogger(PlanillaCerradaListener.class);

    private final BoletaService boletas;

    PlanillaCerradaListener(BoletaService boletas) {
        this.boletas = boletas;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alCerrar(PlanillaCerradaEvento evento) {
        if (CargaHistorica.activa()) {
            return;
        }
        Optional<UUID> anterior = TenantContext.actual();
        TenantContext.establecer(evento.empresaId());
        try {
            boletas.generarMasivo(evento.planillaId());
        } catch (RuntimeException e) {
            LOG.error("No se pudo iniciar la generación de boletas de la planilla {}", evento.planillaId(), e);
        } finally {
            anterior.ifPresentOrElse(TenantContext::establecer, TenantContext::limpiar);
        }
    }
}
