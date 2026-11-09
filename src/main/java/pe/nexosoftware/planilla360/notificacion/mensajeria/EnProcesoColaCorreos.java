package pe.nexosoftware.planilla360.notificacion.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.notificacion.service.EnvioCorreoService;

/** Degradación sin RabbitMQ: el correo se envía en el mismo proceso; un fallo se registra y no afecta a la boleta. */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
class EnProcesoColaCorreos implements ColaCorreos {

    private static final Logger LOG = LoggerFactory.getLogger(EnProcesoColaCorreos.class);

    private final EnvioCorreoService envio;

    EnProcesoColaCorreos(EnvioCorreoService envio) {
        this.envio = envio;
    }

    @Override
    public void encolar(CorreoBoleta correo) {
        try {
            envio.enviarAvisoDeBoleta(correo);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo enviar el aviso de la boleta {}: {}", correo.boletaId(), e.getMessage());
        }
    }
}
