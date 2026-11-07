package pe.nexosoftware.planilla360.identidad.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.excepcion.DemasiadosIntentosException;

/** Limita los intentos de login (Bucket4j en memoria) para frenar fuerza bruta y relleno de credenciales. */
@Component
public class LoginRateLimiter {

    private final RateLimitProperties props;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    public LoginRateLimiter(RateLimitProperties props) {
        this.props = props;
    }

    public void verificar(String ip, String email) {
        consumir("ip:" + ip, props.ipPorMinuto());
        consumir("cred:" + ip + "|" + email.trim().toLowerCase(Locale.ROOT), props.loginPorMinuto());
    }

    private void consumir(String clave, int limite) {
        Bucket bucket = buckets.get(clave, k -> Bucket.builder()
                .addLimit(l -> l.capacity(limite).refillIntervally(limite, Duration.ofMinutes(1)))
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long segundos = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            throw new DemasiadosIntentosException(segundos);
        }
    }
}
