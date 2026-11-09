package pe.nexosoftware.planilla360.boleta.almacenamiento;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(AlmacenamientoProperties.class)
class AlmacenamientoConfig {

    @Bean
    @ConditionalOnProperty(name = "app.storage.tipo", havingValue = "s3")
    AlmacenArchivos almacenS3(AlmacenamientoProperties propiedades) {
        S3Client cliente = S3Client.builder()
                .endpointOverride(URI.create(propiedades.endpoint()))
                .region(Region.of(propiedades.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(propiedades.accessKey(), propiedades.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // R2 no admite las sumas de verificación que el SDK agrega por defecto.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        return new S3AlmacenArchivos(cliente, propiedades.bucket(), propiedades.crearBucket());
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.tipo", havingValue = "memoria", matchIfMissing = true)
    AlmacenArchivos almacenEnMemoria() {
        return new MemoriaAlmacenArchivos();
    }
}
