package pe.nexosoftware.planilla360.boleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pe.nexosoftware.planilla360.boleta.pdf.GeneradorBoletaPdf;
import pe.nexosoftware.planilla360.soporte.PlanillaCerradaBase;
import tools.jackson.databind.JsonNode;

/**
 * Flujo asíncrono real: al cerrar la planilla se encolan las boletas en RabbitMQ, un consumidor genera cada PDF, el
 * aviso va por otra cola y llega a Mailpit. Un fallo permanente agota los reintentos, pasa a la DLQ y deja la boleta
 * FALLIDA hasta que RRHH relance la generación.
 */
@Testcontainers
@DisplayName("Boletas y correos por RabbitMQ")
class BoletaMensajeriaTests extends PlanillaCerradaBase {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Container
    static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.24")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.messaging.enabled", () -> "true");
        registro.add("app.notificaciones.habilitadas", () -> "true");
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registro.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registro.add("spring.mail.host", MAILPIT::getHost);
        registro.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
        registro.add("management.health.rabbit.enabled", () -> "false");
    }

    @MockitoSpyBean
    private GeneradorBoletaPdf generador;

    @BeforeEach
    void escenario() throws Exception {
        reset(generador);
        prepararEmpresaConTrabajadores();
    }

    private JsonNode estado(UUID planilla) throws Exception {
        return json.readTree(leer(e.rrhh(), PLANILLAS + "/" + planilla + "/boletas/estado"));
    }

    /** Direcciones destinatarias de todos los mensajes que hay en Mailpit. */
    private java.util.Set<String> destinatarios() throws Exception {
        HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025)
                        + "/api/v1/messages?limit=100")).build(),
                HttpResponse.BodyHandlers.ofString());
        java.util.Set<String> direcciones = new java.util.TreeSet<>();
        for (JsonNode mensaje : json.readTree(respuesta.body()).get("messages")) {
            assertThat(mensaje.get("Subject").asString()).contains("boleta de pago de 2030-03");
            direcciones.add(mensaje.get("To").get(0).get("Address").asString());
        }
        return direcciones;
    }

    @Test
    @DisplayName("el cierre genera las boletas por la cola y avisa por correo a cada trabajador")
    void flujoCompleto() throws Exception {
        UUID planilla = cerrarPlanillaDeMarzo();

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            JsonNode estado = estado(planilla);
            assertThat(estado.get("total").asInt()).isEqualTo(3);
            assertThat(estado.get("enviadas").asInt()).isEqualTo(3);
        });
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(destinatarios())
                .containsExactlyInAnyOrder("ana@ejemplo.pe", "beto@ejemplo.pe", "carla@ejemplo.pe"));
    }

    @Test
    @DisplayName("un fallo permanente agota los reintentos, va a la DLQ y se recupera al relanzar")
    void reintentosYDlq() throws Exception {
        doThrow(new IllegalStateException("PDF roto")).when(generador).generar(any());

        UUID planilla = cerrarPlanillaDeMarzo();

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            JsonNode estado = estado(planilla);
            assertThat(estado.get("fallidas").asInt()).isEqualTo(3);
            assertThat(estado.get("estado").asString()).isEqualTo("CON_FALLOS");
        });
        verify(generador, atLeast(9)).generar(any());

        doCallRealMethod().when(generador).generar(any());
        mvc.perform(post(PLANILLAS + "/" + planilla + "/boletas/generar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isAccepted());

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            JsonNode estado = estado(planilla);
            assertThat(estado.get("fallidas").asInt()).isZero();
            assertThat(estado.get("enviadas").asInt()).isEqualTo(3);
        });
    }
}
