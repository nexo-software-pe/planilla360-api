package pe.nexosoftware.planilla360.notificacion.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.mensajeria.Colas;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.notificacion.service.EnvioCorreoService;

/** Consume la cola de correos con reintentos; los que agotan los intentos quedan en la DLQ para revisión. */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
class ConsumidorCorreos {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorCorreos.class);

    private final EnvioCorreoService envio;

    ConsumidorCorreos(EnvioCorreoService envio) {
        this.envio = envio;
    }

    @RabbitListener(queues = Colas.CORREOS)
    void enviar(CorreoBoleta correo) {
        TenantContext.establecer(correo.empresaId());
        try {
            envio.enviarAvisoDeBoleta(correo);
        } finally {
            TenantContext.limpiar();
        }
    }

    @RabbitListener(queues = Colas.CORREOS_DLQ)
    void registrarFallido(
            CorreoBoleta correo, @Header(name = "x-exception-message", required = false) String motivo) {
        LOG.error("Correo de la boleta {} agotó los reintentos y queda en la DLQ: {}", correo.boletaId(), motivo);
    }
}
