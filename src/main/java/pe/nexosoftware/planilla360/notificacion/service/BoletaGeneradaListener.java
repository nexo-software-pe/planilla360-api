package pe.nexosoftware.planilla360.notificacion.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.nexosoftware.planilla360.boleta.BoletaGeneradaEvento;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.notificacion.mensajeria.ColaCorreos;
import pe.nexosoftware.planilla360.notificacion.mensajeria.CorreoBoleta;

/** Cuando una boleta queda guardada (transacción confirmada), encola el aviso por correo al trabajador. */
@Component
class BoletaGeneradaListener {

    private final ColaCorreos cola;

    BoletaGeneradaListener(ColaCorreos cola) {
        this.cola = cola;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alGenerarse(BoletaGeneradaEvento evento) {
        Optional<UUID> anterior = TenantContext.actual();
        TenantContext.establecer(evento.empresaId());
        try {
            cola.encolar(new CorreoBoleta(
                    evento.empresaId(), evento.boletaId(), evento.empleadoId(), evento.periodo(), evento.tipo()));
        } finally {
            anterior.ifPresentOrElse(TenantContext::establecer, TenantContext::limpiar);
        }
    }
}
