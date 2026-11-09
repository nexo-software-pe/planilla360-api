package pe.nexosoftware.planilla360.compartido.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;

// Las pruebas de Spring Boot desactivan por defecto la exportación de métricas; aquí se necesita Prometheus.
@TestPropertySource(properties = {
        "management.prometheus.metrics.export.enabled=true",
        "management.endpoint.health.show-details=always"})
@DisplayName("Observabilidad: salud, métricas y traceId")
class ObservabilidadTests extends IntegracionBase {

    private static final String BASIC_CORRECTO = basic("prometheus:clave-de-prueba-metricas");
    private static final String BASIC_INCORRECTO = basic("prometheus:otra-clave");

    private static String basic(String credencial) {
        return "Basic " + Base64.getEncoder().encodeToString(credencial.getBytes());
    }

    @Test
    void livenessYReadinessSonPublicosYReadinessIncluyeLaBaseDeDatos() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(jsonPath("$.components.db.status").value("UP"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(jsonPath("$.components.db").doesNotExist());
    }

    @Test
    void prometheusSinCredencialesResponde401() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusConClaveIncorrectaResponde401() throws Exception {
        mvc.perform(get("/actuator/prometheus").header(HttpHeaders.AUTHORIZATION, BASIC_INCORRECTO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unJwtDeAdministradorNoSirveParaLeerLasMetricas() throws Exception {
        String token = bearer(nuevoUsuario(nuevaEmpresa(), Rol.ADMIN_SAAS));

        mvc.perform(get("/actuator/prometheus").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusConLaCredencialDelRaspadorExponeLasMetricasHttpYDeLaJvm() throws Exception {
        mvc.perform(get("/actuator/health"));

        String cuerpo = mvc.perform(get("/actuator/prometheus").header(HttpHeaders.AUTHORIZATION, BASIC_CORRECTO)
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo)
                .contains("jvm_memory_used_bytes")
                .contains("application=\"planilla360-api\"")
                .contains("http_server_requests_seconds_bucket");
    }

    @Test
    void cadaRespuestaLlevaSuTraceIdAunSiEsUn401() throws Exception {
        mvc.perform(get("/api/v1/empleados")).andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", Matchers.matchesPattern("[0-9a-f]{32}")));
    }

    @Test
    void elTraceIdDelClienteSeConservaEnLaRespuesta() throws Exception {
        mvc.perform(post("/api/v1/auth/login").header("X-Request-Id", "soporte-12345678")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(header().string("X-Trace-Id", "soporte-12345678"));
    }
}
