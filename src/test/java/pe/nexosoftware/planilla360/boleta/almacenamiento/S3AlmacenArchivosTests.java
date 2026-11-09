package pe.nexosoftware.planilla360.boleta.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** El adaptador S3 contra un MinIO real (el mismo protocolo que habla Cloudflare R2). */
@Testcontainers
@DisplayName("Almacén de archivos S3 (MinIO)")
class S3AlmacenArchivosTests {

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("cgr.dev/chainguard/minio:latest")
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "prueba")
            .withEnv("MINIO_ROOT_PASSWORD", "prueba-12345")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    private static AlmacenArchivos almacen(String bucket) {
        AlmacenamientoProperties propiedades = new AlmacenamientoProperties(
                "s3", "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000), bucket, "prueba",
                "prueba-12345", "us-east-1", true);
        return new AlmacenamientoConfig().almacenS3(propiedades);
    }

    @Test
    @DisplayName("crea el bucket si falta, guarda un archivo y lo devuelve igual")
    void guardaYLee() {
        AlmacenArchivos almacen = almacen("boletas-prueba");
        byte[] contenido = "%PDF-contenido".getBytes(StandardCharsets.UTF_8);

        almacen.guardar("empresa/2030-03/boleta.pdf", contenido, "application/pdf");

        assertThat(almacen.leer("empresa/2030-03/boleta.pdf")).contains(contenido);
    }

    @Test
    @DisplayName("una clave inexistente devuelve vacío, no un error")
    void claveInexistente() {
        AlmacenArchivos almacen = almacen("boletas-vacio");
        almacen.guardar("otra.pdf", new byte[] {1}, "application/pdf");

        assertThat(almacen.leer("no-existe.pdf")).isEmpty();
    }

    @Test
    @DisplayName("guardar dos veces la misma clave reemplaza el contenido")
    void reemplaza() {
        AlmacenArchivos almacen = almacen("boletas-reemplazo");
        almacen.guardar("a.pdf", new byte[] {1}, "application/pdf");
        almacen.guardar("a.pdf", new byte[] {2, 2}, "application/pdf");

        assertThat(almacen.leer("a.pdf")).contains(new byte[] {2, 2});
    }

    @Test
    @DisplayName("el almacén en memoria devuelve copias y no comparte el arreglo original")
    void memoriaIsolada() {
        MemoriaAlmacenArchivos memoria = new MemoriaAlmacenArchivos();
        byte[] original = {1, 2, 3};
        memoria.guardar("x", original, "application/pdf");
        original[0] = 9;

        assertThat(memoria.leer("x")).contains(new byte[] {1, 2, 3});
        assertThat(memoria.leer("y")).isEmpty();
    }
}
