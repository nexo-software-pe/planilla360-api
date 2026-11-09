package pe.nexosoftware.planilla360.integracion;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.excepcion.ServicioNoDisponibleException;
import pe.nexosoftware.planilla360.integracion.cliente.IntegracionProperties;
import pe.nexosoftware.planilla360.integracion.cliente.IntegracionProperties.Cortacircuito;
import pe.nexosoftware.planilla360.integracion.cliente.ProveedorConsultas;
import pe.nexosoftware.planilla360.integracion.cliente.ResilienciaExterna;
import pe.nexosoftware.planilla360.integracion.model.TipoCambio;
import pe.nexosoftware.planilla360.integracion.service.ConsultaExternaService;

/**
 * El cliente HTTP, los reintentos y el cortacircuito contra un servidor simulado (WireMock): lo que pasa cuando el
 * servicio externo responde bien, no encuentra el dato, falla una vez, falla siempre o se cae por completo.
 */
@DisplayName("Consultas externas con Resilience4j")
class ConsultaExternaTests {

    private static WireMockServer servidor;
    private ConsultaExternaService servicio;
    private ResilienciaExterna resiliencia;
    private MutableClock reloj;

    /** Reloj que se puede adelantar para probar la caducidad de la caché sin esperar. */
    static final class MutableClock extends Clock {
        private Instant ahora = Instant.parse("2030-04-02T15:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }
    }

    @BeforeAll
    static void arrancarServidor() {
        servidor = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        servidor.start();
    }

    @AfterAll
    static void detenerServidor() {
        servidor.stop();
    }

    @BeforeEach
    void preparar() throws Exception {
        servidor.resetAll();
        reloj = new MutableClock();
        IntegracionProperties propiedades = new IntegracionProperties(
                "http", "http://localhost:" + servidor.port(), Duration.ofMillis(800), 3, Duration.ofMillis(10),
                Duration.ofMinutes(30), new Cortacircuito(3, 50f, Duration.ofSeconds(30)));
        resiliencia = new ResilienciaExterna(propiedades);
        var constructor = Class.forName("pe.nexosoftware.planilla360.integracion.cliente.HttpProveedorConsultas")
                .getDeclaredConstructor(IntegracionProperties.class);
        constructor.setAccessible(true);
        ProveedorConsultas proveedor = (ProveedorConsultas) constructor.newInstance(propiedades);
        servicio = new ConsultaExternaService(proveedor, resiliencia, propiedades, reloj);
    }

    @AfterEach
    void limpiar() {
        servidor.resetAll();
    }

    private static void tipoCambioOk() {
        servidor.stubFor(get(urlEqualTo("/tipo-cambio")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fecha\":\"2030-04-02\",\"moneda\":\"USD\",\"compra\":3.70,\"venta\":3.75}")));
    }

    @Nested
    @DisplayName("respuestas normales")
    class Normales {

        @Test
        @DisplayName("consulta un DNI y un RUC")
        void dniYRuc() {
            servidor.stubFor(get(urlEqualTo("/dni/45678901")).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"dni\":\"45678901\",\"nombres\":\"ANA\",\"apellidoPaterno\":\"RIOS\","
                            + "\"apellidoMaterno\":\"LUNA\"}")));
            servidor.stubFor(get(urlEqualTo("/ruc/20601234565")).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"ruc\":\"20601234565\",\"razonSocial\":\"TEXTILES\",\"estado\":\"ACTIVO\","
                            + "\"condicion\":\"HABIDO\",\"direccion\":\"LIMA\"}")));

            assertThat(servicio.dni("45678901").nombres()).isEqualTo("ANA");
            assertThat(servicio.ruc("20601234565").razonSocial()).isEqualTo("TEXTILES");
        }

        @Test
        @DisplayName("un 404 es 'no existe' y no se reintenta")
        void noExiste() {
            servidor.stubFor(get(urlEqualTo("/dni/00000000")).willReturn(aResponse().withStatus(404)));

            assertThatThrownBy(() -> servicio.dni("00000000")).isInstanceOf(RecursoNoEncontradoException.class);
            servidor.verify(1, getRequestedFor(urlEqualTo("/dni/00000000")));
        }

        @Test
        @DisplayName("un 400 es una petición rechazada y no se reintenta")
        void peticionRechazada() {
            servidor.stubFor(get(urlEqualTo("/dni/11111111")).willReturn(aResponse().withStatus(400)));

            assertThatThrownBy(() -> servicio.dni("11111111")).isInstanceOf(ReglaDeNegocioException.class);
            servidor.verify(1, getRequestedFor(urlEqualTo("/dni/11111111")));
        }

        @Test
        @DisplayName("valida el formato antes de llamar al servicio")
        void validaFormato() {
            assertThatThrownBy(() -> servicio.dni("123")).isInstanceOf(ReglaDeNegocioException.class);
            assertThatThrownBy(() -> servicio.ruc("20601234560")).isInstanceOf(ReglaDeNegocioException.class);
            assertThat(servidor.getAllServeEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fallos del servicio externo")
    class Fallos {

        @Test
        @DisplayName("reintenta un fallo pasajero y entrega la respuesta")
        void reintentaYSeRecupera() {
            servidor.stubFor(get(urlEqualTo("/tipo-cambio")).inScenario("pasajero")
                    .whenScenarioStateIs(Scenario.STARTED).willReturn(aResponse().withStatus(503))
                    .willSetStateTo("recuperado"));
            servidor.stubFor(get(urlEqualTo("/tipo-cambio")).inScenario("pasajero")
                    .whenScenarioStateIs("recuperado").willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"fecha\":\"2030-04-02\",\"moneda\":\"USD\",\"compra\":3.70,\"venta\":3.75}")));

            TipoCambio tipo = servicio.tipoCambio();

            assertThat(tipo.venta()).isEqualByComparingTo("3.75");
            assertThat(tipo.desactualizado()).isFalse();
            servidor.verify(moreThanOrExactly(2), getRequestedFor(urlEqualTo("/tipo-cambio")));
        }

        @Test
        @DisplayName("si siempre falla agota los intentos y responde 503")
        void agotaIntentos() {
            servidor.stubFor(get(urlEqualTo("/dni/45678901")).willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> servicio.dni("45678901")).isInstanceOf(ServicioNoDisponibleException.class);
            servidor.verify(3, getRequestedFor(urlEqualTo("/dni/45678901")));
        }

        @Test
        @DisplayName("un timeout cuenta como fallo transitorio")
        void timeout() {
            servidor.stubFor(get(urlEqualTo("/dni/45678901")).willReturn(aResponse().withStatus(200).withFixedDelay(2500)));

            assertThatThrownBy(() -> servicio.dni("45678901")).isInstanceOf(ServicioNoDisponibleException.class);
        }

        @Test
        @DisplayName("tras varios fallos el circuito se abre y deja de llamar al servicio")
        void circuitoAbierto() {
            servidor.stubFor(get(urlEqualTo("/dni/45678901")).willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> servicio.dni("45678901")).isInstanceOf(ServicioNoDisponibleException.class);
            assertThat(resiliencia.estadoDelCircuito()).isEqualTo(CircuitBreaker.State.OPEN);
            int llamadas = servidor.getAllServeEvents().size();

            assertThatThrownBy(() -> servicio.dni("45678901")).isInstanceOf(ServicioNoDisponibleException.class);

            assertThat(servidor.getAllServeEvents()).hasSize(llamadas);
        }
    }

    @Nested
    @DisplayName("tipo de cambio")
    class TipoDeCambio {

        @Test
        @DisplayName("se guarda mientras no caduque y no vuelve a llamar al servicio")
        void usaCache() {
            tipoCambioOk();

            servicio.tipoCambio();
            servicio.tipoCambio();

            servidor.verify(1, getRequestedFor(urlEqualTo("/tipo-cambio")));
        }

        @Test
        @DisplayName("si el servicio cae, devuelve el último valor marcado como desactualizado")
        void ultimoValorConocido() {
            tipoCambioOk();
            servicio.tipoCambio();
            servidor.resetAll();
            servidor.stubFor(get(urlEqualTo("/tipo-cambio")).willReturn(aResponse().withStatus(500)));
            reloj.avanzar(Duration.ofHours(1));

            TipoCambio tipo = servicio.tipoCambio();

            assertThat(tipo.desactualizado()).isTrue();
            assertThat(tipo.venta()).isEqualByComparingTo("3.75");
        }

        @Test
        @DisplayName("sin valor previo y con el servicio caído responde 503")
        void sinValorPrevio() {
            servidor.stubFor(get(urlEqualTo("/tipo-cambio")).willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> servicio.tipoCambio()).isInstanceOf(ServicioNoDisponibleException.class);
        }
    }
}
