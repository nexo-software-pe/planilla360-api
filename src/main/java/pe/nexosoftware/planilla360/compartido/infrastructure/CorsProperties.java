package pe.nexosoftware.planilla360.compartido.infrastructure;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Orígenes permitidos por CORS ({@code CORS_ALLOWED_ORIGINS}, separados por coma). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
