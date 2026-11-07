package pe.nexosoftware.planilla360.identidad.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Límites de intentos de login por minuto: por IP y por combinación IP + correo. */
@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(int loginPorMinuto, int ipPorMinuto) {
}
