package pe.nexosoftware.planilla360.boleta.almacenamiento;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Adapter de {@link AlmacenArchivos} sobre el protocolo S3 (MinIO y Cloudflare R2 lo hablan). */
public class S3AlmacenArchivos implements AlmacenArchivos {

    private final S3Client cliente;
    private final String bucket;
    private final boolean crearBucketSiFalta;
    private final AtomicBoolean bucketVerificado = new AtomicBoolean(false);

    public S3AlmacenArchivos(S3Client cliente, String bucket, boolean crearBucketSiFalta) {
        this.cliente = cliente;
        this.bucket = bucket;
        this.crearBucketSiFalta = crearBucketSiFalta;
    }

    @Override
    public void guardar(String clave, byte[] contenido, String tipoContenido) {
        asegurarBucket();
        cliente.putObject(
                PutObjectRequest.builder().bucket(bucket).key(clave).contentType(tipoContenido).build(),
                RequestBody.fromBytes(contenido));
    }

    @Override
    public Optional<byte[]> leer(String clave) {
        try {
            ResponseBytes<GetObjectResponse> respuesta =
                    cliente.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(clave).build());
            return Optional.of(respuesta.asByteArray());
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            return Optional.empty();
        }
    }

    private void asegurarBucket() {
        if (!crearBucketSiFalta || bucketVerificado.get()) {
            return;
        }
        try {
            cliente.headBucket(b -> b.bucket(bucket));
        } catch (NoSuchBucketException e) {
            cliente.createBucket(b -> b.bucket(bucket));
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw e;
            }
            cliente.createBucket(b -> b.bucket(bucket));
        }
        bucketVerificado.set(true);
    }
}
