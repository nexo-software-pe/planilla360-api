package pe.nexosoftware.planilla360.boleta.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.boleta.service.GeneracionBoletaService;
import pe.nexosoftware.planilla360.compartido.mensajeria.Colas;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;

/**
 * Consume la cola de generación. Si lanza una excepción, el contenedor reintenta (con espera creciente) y, al
 * agotar los intentos, el mensaje va a la DLQ, que el segundo listener usa para marcar la boleta como FALLIDA.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
class ConsumidorBoletas {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorBoletas.class);

    private final GeneracionBoletaService generacion;

    ConsumidorBoletas(GeneracionBoletaService generacion) {
        this.generacion = generacion;
    }

    @RabbitListener(queues = Colas.BOLETAS)
    void generar(TrabajoBoleta trabajo) {
        TenantContext.establecer(trabajo.empresaId());
        try {
            generacion.generar(trabajo.planillaId(), trabajo.empleadoId());
        } finally {
            TenantContext.limpiar();
        }
    }

    @RabbitListener(queues = Colas.BOLETAS_DLQ)
    void marcarFallida(
            TrabajoBoleta trabajo, @Header(name = "x-exception-message", required = false) String motivo) {
        LOG.warn("Boleta del empleado {} en la DLQ: {}", trabajo.empleadoId(), motivo);
        TenantContext.establecer(trabajo.empresaId());
        try {
            generacion.marcarFallida(trabajo.planillaId(), trabajo.empleadoId(),
                    motivo == null ? "Se agotaron los reintentos" : motivo);
        } finally {
            TenantContext.limpiar();
        }
    }
}
