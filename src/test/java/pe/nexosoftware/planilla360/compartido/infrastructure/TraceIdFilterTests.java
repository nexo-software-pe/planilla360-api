package pe.nexosoftware.planilla360.compartido.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("TraceIdFilter")
class TraceIdFilterTests {

    private static final String TRACE_W3C = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final TraceIdFilter filtro = new TraceIdFilter();

    /** Ejecuta el filtro y devuelve (traceId visto dentro de la cadena, respuesta). */
    private record Resultado(String traceIdEnLaCadena, MockHttpServletResponse respuesta) {
    }

    private Resultado ejecutar(MockHttpServletRequest peticion) throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicReference<String> visto = new AtomicReference<>();
        filtro.doFilter(peticion, respuesta, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                visto.set(MDC.get("traceId"));
            }
        });
        return new Resultado(visto.get(), respuesta);
    }

    @Test
    void generaUnTraceIdDe32HexadecimalesYLoDevuelveEnLaCabecera() throws Exception {
        Resultado r = ejecutar(new MockHttpServletRequest());

        assertThat(r.traceIdEnLaCadena()).matches("[0-9a-f]{32}");
        assertThat(r.respuesta().getHeader("X-Trace-Id")).isEqualTo(r.traceIdEnLaCadena());
    }

    @Test
    void cadaPeticionRecibeUnTraceIdDistinto() throws Exception {
        assertThat(ejecutar(new MockHttpServletRequest()).traceIdEnLaCadena())
                .isNotEqualTo(ejecutar(new MockHttpServletRequest()).traceIdEnLaCadena());
    }

    @Test
    void respetaElTraceIdDeLaCabeceraW3cTraceparent() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("traceparent", "00-" + TRACE_W3C + "-00f067aa0ba902b7-01");

        assertThat(ejecutar(peticion).traceIdEnLaCadena()).isEqualTo(TRACE_W3C);
    }

    @Test
    void ignoraUnTraceparentMalFormadoOConTraceIdEnCeros() throws Exception {
        MockHttpServletRequest malFormado = new MockHttpServletRequest();
        malFormado.addHeader("traceparent", "no-es-w3c");
        MockHttpServletRequest enCeros = new MockHttpServletRequest();
        enCeros.addHeader("traceparent", "00-00000000000000000000000000000000-00f067aa0ba902b7-01");

        assertThat(ejecutar(malFormado).traceIdEnLaCadena()).matches("[0-9a-f]{32}");
        assertThat(ejecutar(enCeros).traceIdEnLaCadena()).matches("[0-9a-f]{32}").isNotEqualTo("0".repeat(32));
    }

    @Test
    void respetaXRequestIdValido() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Request-Id", "req-1234-abcd");

        assertThat(ejecutar(peticion).traceIdEnLaCadena()).isEqualTo("req-1234-abcd");
    }

    @Test
    void descartaUnXRequestIdQuePodriaInyectarTextoEnLosLogs() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Request-Id", "x\",\"nivel\":\"ERROR");

        assertThat(ejecutar(peticion).traceIdEnLaCadena()).matches("[0-9a-f]{32}");
    }

    @Test
    void dejaUnaLineaDeAccesoConElTraceIdSinLaCadenaDeConsulta(CapturedOutput salida) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/v1/empleados");
        peticion.setQueryString("dni=99887766");
        peticion.addHeader("X-Request-Id", "acceso-12345678");

        ejecutar(peticion);

        assertThat(salida.getOut()).contains("GET /api/v1/empleados -> 200").doesNotContain("99887766");
    }

    @Test
    void limpiaElMdcAlTerminarAunqueLaCadenaFalle() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        try {
            filtro.doFilter(peticion, respuesta, new MockFilterChain() {
                @Override
                public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                    throw new IllegalStateException("falla a propósito");
                }
            });
        } catch (Exception esperado) {
            // lo que importa es lo que queda en el MDC
        }

        assertThat(MDC.get("traceId")).isNull();
    }
}
