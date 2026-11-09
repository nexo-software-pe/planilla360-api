package pe.nexosoftware.planilla360;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.nexosoftware.planilla360.soporte.TestcontainersConfig;
import tools.jackson.databind.json.JsonMapper;

/**
 * La regla de oro de los seeds: los datos ficticios del perfil local son coherentes con el sistema. Las planillas
 * históricas se cerraron con las mismas validaciones que exige el cierre (asistencia completa, sin vacaciones
 * pendientes, regímenes definidos) y la del mes en curso queda en borrador, lista para calcularse y cerrarse.
 */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed/local")
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Import(TestcontainersConfig.class)
@DisplayName("Seed del perfil local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeedLocalTests {

    private static final String CLAVE = "Planilla360-Local1";
    private static final String TEXTILES = "11111111-1111-4111-8111-111111111111";
    private static final String ESPIGA = "22222222-2222-4222-8222-222222222222";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    private static YearMonth mesActual() {
        return YearMonth.now(ZoneId.of("America/Lima"));
    }

    @Test
    @Order(1)
    @DisplayName("cada empresa tiene seis meses de planillas mensuales cerradas y la del mes en curso en borrador")
    void planillasDelSeed() {
        for (String empresa : List.of(TEXTILES, ESPIGA)) {
            for (int atras = 1; atras <= 6; atras++) {
                String periodo = mesActual().minusMonths(atras).toString();
                String estado = jdbc.queryForObject(
                        "select estado from planilla where tenant_id = ?::uuid and periodo = ? and tipo = 'MENSUAL'",
                        String.class, empresa, periodo);
                assertThat(estado).as("%s %s", empresa, periodo).isEqualTo("CERRADA");
            }
            String actual = jdbc.queryForObject(
                    "select estado from planilla where tenant_id = ?::uuid and periodo = ? and tipo = 'MENSUAL'",
                    String.class, empresa, mesActual().toString());
            assertThat(actual).isEqualTo("BORRADOR");
        }
    }

    @Test
    @Order(2)
    @DisplayName("la asistencia cubre seis meses y el mes en curso completo, con tardanzas, horas extra y faltas")
    void asistenciaDelSeed() {
        LocalDate desde = mesActual().minusMonths(6).atDay(1);
        LocalDate hasta = mesActual().atEndOfMonth();
        for (String empresa : List.of(TEXTILES, ESPIGA)) {
            Map<String, Object> resumen = jdbc.queryForMap("""
                    select min(fecha) as desde, max(fecha) as hasta,
                           count(*) filter (where estado = 'FALTA') as faltas,
                           count(*) filter (where estado = 'TARDANZA') as tardanzas,
                           count(*) filter (where minutos_extra > 0) as con_extra,
                           count(*) filter (where estado = 'INCOMPLETA') as incompletas
                      from asistencia_diaria where tenant_id = ?::uuid""", empresa);
            assertThat(((java.sql.Date) resumen.get("desde")).toLocalDate()).isBeforeOrEqualTo(desde.plusDays(31));
            assertThat(((java.sql.Date) resumen.get("hasta")).toLocalDate()).isEqualTo(hasta);
            assertThat((Long) resumen.get("faltas")).isPositive();
            assertThat((Long) resumen.get("tardanzas")).isPositive();
            assertThat((Long) resumen.get("con_extra")).isPositive();
            assertThat((Long) resumen.get("incompletas")).isZero();
        }
        Integer horasExtraAprobadas = jdbc.queryForObject(
                "select count(*) from hora_extra where estado = 'APROBADA'", Integer.class);
        assertThat(horasExtraAprobadas).isPositive();
    }

    @Test
    @Order(3) // al final: cierra la planilla del mes en curso
    @DisplayName("el cierre del mes en curso pasa las validaciones y genera una planilla cerrada")
    void calcularYCerrarElMesActual() throws Exception {
        String token = login("rrhh@textilesandinos.ejemplo.pe");
        String listado = mvc.perform(get("/api/v1/planillas?periodo=" + mesActual() + "&tipo=MENSUAL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(listado).get("contenido").get(0).get("id").asString();

        mvc.perform(post("/api/v1/planillas/" + id + "/calcular").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        String validacion = mvc.perform(get("/api/v1/planillas/" + id + "/validaciones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(validacion).get("valida").asBoolean()).isTrue();
        mvc.perform(post("/api/v1/planillas/" + id + "/cerrar").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "seed-" + id))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select estado from planilla where id = ?::uuid", String.class, id))
                .isEqualTo("CERRADA");
    }

    private String login(String email) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", CLAVE))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("accessToken").asString();
    }
}
