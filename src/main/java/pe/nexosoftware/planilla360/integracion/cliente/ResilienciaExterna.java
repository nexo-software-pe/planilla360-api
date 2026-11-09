package pe.nexosoftware.planilla360.integracion.cliente;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.excepcion.ServicioNoDisponibleException;

/**
 * Reintentos y cortacircuito (Resilience4j) alrededor de una llamada externa. El reintento envuelve al
 * cortacircuito: cada intento fallido cuenta en la ventana y, con el circuito abierto, la llamada se rechaza al
 * instante (503) sin esperar el timeout. Solo los fallos transitorios se reintentan y cuentan.
 */
@Component
@EnableConfigurationProperties(IntegracionProperties.class)
public class ResilienciaExterna {

    private final CircuitBreaker cortacircuito;
    private final Retry reintento;

    public ResilienciaExterna(IntegracionProperties p) {
        this.cortacircuito = CircuitBreaker.of("servicio-externo", CircuitBreakerConfig.custom()
                .slidingWindowSize(p.cortacircuito().ventana())
                .minimumNumberOfCalls(p.cortacircuito().ventana())
                .failureRateThreshold(p.cortacircuito().tasaFallos())
                .waitDurationInOpenState(p.cortacircuito().esperaAbierto())
                .recordExceptions(ErrorTransitorioException.class)
                .build());
        this.reintento = Retry.of("servicio-externo", RetryConfig.custom()
                .maxAttempts(p.reintentos())
                .waitDuration(p.esperaReintento())
                .retryExceptions(ErrorTransitorioException.class)
                .build());
    }

    public <T> T ejecutar(Supplier<T> llamada) {
        Supplier<T> protegida =
                Retry.decorateSupplier(reintento, CircuitBreaker.decorateSupplier(cortacircuito, llamada));
        try {
            return protegida.get();
        } catch (CallNotPermittedException e) {
            throw new ServicioNoDisponibleException(
                    "El servicio de consultas externas no responde; inténtalo de nuevo en unos minutos.");
        } catch (ErrorTransitorioException e) {
            throw new ServicioNoDisponibleException("El servicio de consultas externas no pudo atender la solicitud.");
        }
    }

    public CircuitBreaker.State estadoDelCircuito() {
        return cortacircuito.getState();
    }
}
