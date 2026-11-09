package pe.nexosoftware.planilla360.boleta.almacenamiento;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración {@code app.storage.*}.
 *
 * @param tipo {@code s3} (MinIO o R2) o {@code memoria}
 * @param region MinIO acepta cualquiera; R2 pide {@code auto}
 * @param crearBucket crea el bucket al primer uso si no existe (cómodo en local)
 */
@ConfigurationProperties("app.storage")
public record AlmacenamientoProperties(
        String tipo,
        String endpoint,
        String bucket,
        String accessKey,
        String secretKey,
        String region,
        boolean crearBucket) {
}
