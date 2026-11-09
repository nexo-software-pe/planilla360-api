package pe.nexosoftware.planilla360.boleta.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.boleta.service.GeneracionBoletaService;

/**
 * Degradación sin RabbitMQ ({@code app.messaging.enabled=false}): la boleta se genera en el mismo proceso. Un fallo
 * deja la boleta en FALLIDA (no hay reintentos automáticos); se reintenta con el endpoint de generación.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
class EnProcesoColaBoletas implements ColaBoletas {

    private static final Logger LOG = LoggerFactory.getLogger(EnProcesoColaBoletas.class);

    private final GeneracionBoletaService generacion;

    EnProcesoColaBoletas(GeneracionBoletaService generacion) {
        this.generacion = generacion;
    }

    @Override
    public void encolar(TrabajoBoleta trabajo) {
        try {
            generacion.generar(trabajo.planillaId(), trabajo.empleadoId());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo generar la boleta del empleado {}: {}", trabajo.empleadoId(), e.getMessage());
            generacion.marcarFallida(trabajo.planillaId(), trabajo.empleadoId(), e.getMessage());
        }
    }
}
