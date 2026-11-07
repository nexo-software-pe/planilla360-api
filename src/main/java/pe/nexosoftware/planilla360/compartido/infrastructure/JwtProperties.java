package pe.nexosoftware.planilla360.compartido.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los tokens. El secreto llega siempre por variable de entorno ({@code JWT_SECRET}).
 * Solo en local/test se permite generar uno efímero al arrancar ({@code generarSiFalta}).
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret, String issuer, Duration accessTtl, Duration refreshTtl, boolean generarSiFalta) {
}
