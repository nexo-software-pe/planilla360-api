package pe.nexosoftware.planilla360.asistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.nexosoftware.planilla360.asistencia.infrastructure.scheduling.ConsolidacionDiariaJob;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.soporte.PersonalBase;
import tools.jackson.databind.JsonNode;

/** El job nocturno consolida a todas las empresas y ShedLock evita que dos instancias lo ejecuten a la vez. */
class ConsolidacionDiariaJobTests extends PersonalBase {

    private static final LocalDate DIA = LocalDate.of(2030, 3, 12);

    @Autowired
    private ConsolidacionDiariaJob job;

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void sinBloqueos() {
        // No se borran las filas: ShedLock recuerda que ya las creó. Se vencen para liberar los bloqueos.
        jdbc.update("update shedlock set lock_until = now() - interval '1 hour'");
        ahoraEs("2030-03-13T00:30");
    }

    private UUID empleadoConHorario(Escenario e, String nombre) throws Exception {
        UUID empleadoId = crearEmpleado(e, nombre, "Del Job", "2024-01-01");
        UUID horario = crearId(e.rrhh(), "/api/v1/horarios", Map.of(
                "nombre", "Oficina", "entrada", "08:00", "salida", "17:00", "toleranciaMin", 10,
                "dias", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")));
        mvc.perform(post("/api/v1/horarios/" + horario + "/asignaciones").header("Authorization", bearer(e.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("empleadoId", empleadoId.toString(), "desde", "2024-01-01"))))
                .andExpect(status().isNoContent());
        return empleadoId;
    }

    private JsonNode asistencia(Escenario e, UUID empleadoId) throws Exception {
        return json.readTree(leer(e.rrhh(), "/api/v1/asistencia?empleadoId=" + empleadoId + "&mes=2030-03"));
    }

    @Test
    void consolidaLasEmpresasActivasYDejaElContextoLimpio() throws Exception {
        Escenario a = nuevoEscenario();
        Escenario b = nuevoEscenario();
        UUID empleadoA = empleadoConHorario(a, "Ana");
        UUID empleadoB = empleadoConHorario(b, "Berta");

        boolean ejecutado = job.ejecutar(DIA);

        assertThat(ejecutado).isTrue();
        assertThat(TenantContext.actual()).isEmpty();
        assertThat(asistencia(a, empleadoA)).hasSize(1);
        assertThat(asistencia(a, empleadoA).get(0).get("estado").asString()).isEqualTo("FALTA");
        assertThat(asistencia(b, empleadoB)).hasSize(1);
    }

    @Test
    void siOtraInstanciaTieneElBloqueoElJobNoHaceNada() throws Exception {
        Escenario a = nuevoEscenario();
        UUID empleadoA = empleadoConHorario(a, "Ana");
        Optional<SimpleLock> ajeno = lockProvider.lock(new LockConfiguration(
                Instant.now(), ConsolidacionDiariaJob.NOMBRE_BLOQUEO, Duration.ofMinutes(5), Duration.ZERO));
        assertThat(ajeno).isPresent();

        assertThat(job.ejecutar(DIA)).isFalse();
        assertThat(asistencia(a, empleadoA)).isEmpty();

        ajeno.orElseThrow().unlock();
        assertThat(job.ejecutar(DIA)).isTrue();
        assertThat(asistencia(a, empleadoA)).hasSize(1);
    }

    @Test
    void unaSegundaEjecucionInmediataQuedaBloqueadaPorElTiempoMinimo() throws Exception {
        assertThat(job.ejecutar(DIA)).isTrue();

        assertThat(job.ejecutar(DIA)).isFalse();
    }
}
