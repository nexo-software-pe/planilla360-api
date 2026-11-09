package pe.nexosoftware.planilla360.integracion.cliente;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración {@code app.integracion.*}. {@code modo}: {@code http} (servicio externo) o {@code stub}. */
@ConfigurationProperties("app.integracion")
public record IntegracionProperties(
        String modo,
        String baseUrl,
        Duration timeout,
        int reintentos,
        Duration esperaReintento,
        Duration cacheTipoCambio,
        Cortacircuito cortacircuito) {

    /** @param tasaFallos porcentaje de fallos de la ventana a partir del cual se abre el circuito */
    public record Cortacircuito(int ventana, float tasaFallos, Duration esperaAbierto) {
    }
}
