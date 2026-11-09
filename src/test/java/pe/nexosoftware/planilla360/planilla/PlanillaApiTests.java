package pe.nexosoftware.planilla360.planilla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.planilla.infrastructure.scheduling.PrecalculoPlanillaJob;
import pe.nexosoftware.planilla360.soporte.PersonalBase;
import tools.jackson.databind.JsonNode;

/**
 * Planilla de punta a punta por la API: crear, calcular, validar, cerrar de forma idempotente, anular, pagar,
 * gratificación, CTS, job de pre-cálculo, permisos por rol y aislamiento entre empresas.
 */
@RecordApplicationEvents
@DisplayName("API de planillas")
class PlanillaApiTests extends PersonalBase {

    private static final String PLANILLAS = "/api/v1/planillas";

    @Autowired
    private ApplicationEvents eventos;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PrecalculoPlanillaJob job;

    private Escenario e;
    private Usuario admin;
    private UUID ana;
    private UUID beto;
    private UUID carla;

    @BeforeEach
    void escenario() throws Exception {
        ahoraEs("2030-04-02T10:00");
        e = nuevoEscenario();
        admin = nuevoUsuario(e.empresa(), Rol.ADMIN_EMPRESA);
        UUID afp = UUID.fromString(json.readTree(leer(e.rrhh(), "/api/v1/afps")).get(0).get("id").asString());
        ana = empleadoConContrato("Ana", "2024-01-01", "3000.00", null);
        beto = empleadoConContrato("Beto", "2024-01-01", "5000.00", afp);
        carla = empleadoConContrato("Carla", "2030-03-16", "2000.00", null);
        mvcPost(e.rrhh(), "/api/v1/empleados/" + ana + "/familiares",
                Map.of("parentesco", "HIJO", "nombres", "Mateo", "fechaNacimiento", "2020-04-02", "estudia", false))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- ayudas

    private UUID empleadoConContrato(String nombre, String ingreso, String sueldo, UUID afp) throws Exception {
        UUID id = crearEmpleado(e, nombre, "Planilla", ingreso);
        Map<String, Object> contrato = new LinkedHashMap<>();
        contrato.put("tipo", "INDEFINIDO");
        contrato.put("fechaInicio", ingreso);
        contrato.put("sueldoBasico", sueldo);
        contrato.put("jornadaSemanal", 48);
        contrato.put("regimenPensionario", afp == null ? "ONP" : "AFP");
        if (afp != null) {
            contrato.put("afpId", afp.toString());
            contrato.put("tipoComision", "FLUJO");
        }
        mvcPost(e.rrhh(), "/api/v1/empleados/" + id + "/contratos", contrato).andExpect(status().isCreated());
        return id;
    }

    private ResultActions mvcPost(Usuario como, String ruta, Object cuerpo) throws Exception {
        return mvc.perform(post(ruta).header("Authorization", bearer(como))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    private ResultActions cerrarCon(Usuario como, UUID planilla, String clave) throws Exception {
        return mvc.perform(post(PLANILLAS + "/" + planilla + "/cerrar")
                .header("Authorization", bearer(como))
                .header("Idempotency-Key", clave));
    }

    private JsonNode leerJson(ResultActions accion) throws Exception {
        return json.readTree(accion.andReturn().getResponse().getContentAsString());
    }

    private UUID crearPlanilla(String periodo, String tipo) throws Exception {
        JsonNode creada = leerJson(mvcPost(e.rrhh(), PLANILLAS, Map.of("periodo", periodo, "tipo", tipo))
                .andExpect(status().isCreated()));
        return UUID.fromString(creada.get("id").asString());
    }

    /** Asigna un horario de oficina y consolida el último día del mes para que la asistencia esté completa. */
    private void completarAsistenciaDeMarzo() throws Exception {
        UUID horario = crearId(e.rrhh(), "/api/v1/horarios", Map.of(
                "nombre", "Oficina", "entrada", "08:00", "salida", "17:00", "toleranciaMin", 10,
                "dias", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")));
        for (UUID empleado : List.of(ana, beto)) {
            mvcPost(e.rrhh(), "/api/v1/horarios/" + horario + "/asignaciones",
                    Map.of("empleadoId", empleado.toString(), "desde", "2024-01-01"))
                    .andExpect(status().isNoContent());
        }
        mvcPost(e.rrhh(), "/api/v1/horarios/" + horario + "/asignaciones",
                Map.of("empleadoId", carla.toString(), "desde", "2030-03-16")).andExpect(status().isNoContent());
        mvcPost(e.rrhh(), "/api/v1/asistencia/consolidaciones", Map.of("fecha", "2030-03-31"))
                .andExpect(status().isOk());
    }

    private static BigDecimal monto(JsonNode nodo, String campo) {
        return new BigDecimal(nodo.get(campo).asString());
    }

    private JsonNode lineaDe(JsonNode lineas, UUID empleado) {
        for (JsonNode linea : lineas) {
            if (empleado.toString().equals(linea.get("empleadoId").asString())) {
                return linea;
            }
        }
        throw new AssertionError("No hay línea del empleado " + empleado);
    }

    // ---------------------------------------------------------------- cálculo

    @Test
    void calculaLaPlanillaMensualConTodosSusConceptos() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");

        JsonNode calculada = leerJson(mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of())
                .andExpect(status().isOk()));

        assertThat(calculada.get("estado").asString()).isEqualTo("CALCULADA");
        assertThat(calculada.get("trabajadores").asInt()).isEqualTo(3);

        JsonNode lineas = json.readTree(leer(e.rrhh(), PLANILLAS + "/" + planilla + "/lineas"));
        JsonNode deAna = lineaDe(lineas, ana);
        assertThat(monto(deAna, "basico")).isEqualByComparingTo("3000.00");
        assertThat(monto(deAna, "asignacionFamiliar")).isEqualByComparingTo("113.00");
        assertThat(monto(deAna, "descuentoPension")).isEqualByComparingTo("404.69");
        assertThat(monto(deAna, "retencion5ta")).isEqualByComparingTo("3.11");
        assertThat(monto(deAna, "neto")).isEqualByComparingTo("2705.20");
        assertThat(deAna.get("conceptos")).isNotEmpty();
        assertThat(deAna.get("conceptos").get(0).get("codigo").asString()).isEqualTo("BASICO");

        assertThat(lineaDe(lineas, beto).get("regimenPensionario").asString()).startsWith("AFP");
        assertThat(monto(lineaDe(lineas, carla), "basico")).isEqualByComparingTo("1032.26");
    }

    @Test
    void recalcularReemplazaLasLineasSinDuplicarlas() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");

        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of()).andExpect(status().isOk());
        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of()).andExpect(status().isOk());

        assertThat(json.readTree(leer(e.rrhh(), PLANILLAS + "/" + planilla + "/lineas"))).hasSize(3);
        assertThat(jdbc.queryForObject(
                        "select count(*) from linea_planilla where planilla_id = ?", Integer.class, planilla))
                .isEqualTo(3);
    }

    @Test
    void noSePuedeTenerDosPlanillasVigentesDelMismoPeriodoYTipo() throws Exception {
        crearPlanilla("2030-03", "MENSUAL");

        mvcPost(e.rrhh(), PLANILLAS, Map.of("periodo", "2030-03", "tipo", "MENSUAL"))
                .andExpect(status().isConflict());
    }

    @Test
    void noSePuedeCrearLaPlanillaDeUnPeriodoFuturo() throws Exception {
        mvcPost(e.rrhh(), PLANILLAS, Map.of("periodo", "2030-05", "tipo", "MENSUAL"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void elPeriodoDebeTenerElFormatoAnioMes() throws Exception {
        mvcPost(e.rrhh(), PLANILLAS, Map.of("periodo", "marzo-2030")).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- validaciones y cierre

    @Test
    void laValidacionReunePendientesYElCierreSeRechazaMientrasLosHaya() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");
        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of()).andExpect(status().isOk());

        JsonNode validacion = json.readTree(leer(e.rrhh(), PLANILLAS + "/" + planilla + "/validaciones"));

        assertThat(validacion.get("valida").asBoolean()).isFalse();
        assertThat(validacion.get("incidencias").get(0).get("codigo").asString()).isEqualTo("ASISTENCIA_INCOMPLETA");
        cerrarCon(e.rrhh(), planilla, "clave-1").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cierraYRepetirElCierreConLaMismaClaveNoVuelveAPublicarElEvento() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");
        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of()).andExpect(status().isOk());
        completarAsistenciaDeMarzo();

        JsonNode cerrada = leerJson(cerrarCon(e.rrhh(), planilla, "cierre-marzo").andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotent-Replayed")));
        assertThat(cerrada.get("estado").asString()).isEqualTo("CERRADA");

        JsonNode repetida = leerJson(cerrarCon(e.rrhh(), planilla, "cierre-marzo").andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true")));
        assertThat(repetida.get("estado").asString()).isEqualTo("CERRADA");
        assertThat(repetida.get("cerradaEn").asString()).isEqualTo(cerrada.get("cerradaEn").asString());

        long publicados = eventos.stream(PlanillaCerradaEvento.class)
                .filter(ev -> ev.planillaId().equals(planilla))
                .count();
        assertThat(publicados).isEqualTo(1);
    }

    @Test
    void cerrarConOtraClaveUnaPlanillaYaCerradaEsUnConflictoDeNegocio() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");
        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of()).andExpect(status().isOk());
        completarAsistenciaDeMarzo();
        cerrarCon(e.rrhh(), planilla, "cierre-1").andExpect(status().isOk());

        cerrarCon(e.rrhh(), planilla, "cierre-2").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void laMismaClaveNoPuedeCerrarDosPlanillasDistintas() throws Exception {
        UUID marzo = crearPlanilla("2030-03", "MENSUAL");
        mvcPost(e.rrhh(), PLANILLAS + "/" + marzo + "/calcular", Map.of()).andExpect(status().isOk());
        completarAsistenciaDeMarzo();
        cerrarCon(e.rrhh(), marzo, "clave-unica").andExpect(status().isOk());
        ahoraEs("2030-07-02T10:00");
        JsonNode gratificacion = leerJson(mvcPost(e.rrhh(), PLANILLAS + "/gratificacion",
                Map.of("periodo", "2030-07", "simular", false)).andExpect(status().isCreated()));

        cerrarCon(e.rrhh(), UUID.fromString(gratificacion.get("id").asString()), "clave-unica")
                .andExpect(status().isConflict());
    }

    @Test
    void cerrarExigeLaCabeceraIdempotencyKey() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");

        mvc.perform(post(PLANILLAS + "/" + planilla + "/cerrar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- anulación y pago

    private UUID planillaCerrada() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");
        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/calcular", Map.of()).andExpect(status().isOk());
        completarAsistenciaDeMarzo();
        cerrarCon(e.rrhh(), planilla, "cierre-" + planilla).andExpect(status().isOk());
        return planilla;
    }

    @Test
    void soloElAdministradorDeLaEmpresaPuedeAnular() throws Exception {
        UUID planilla = planillaCerrada();

        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/anular", Map.of("motivo", "Error de cálculo"))
                .andExpect(status().isForbidden());

        JsonNode anulada = leerJson(mvcPost(admin, PLANILLAS + "/" + planilla + "/anular",
                Map.of("motivo", "Error de cálculo")).andExpect(status().isOk()));
        assertThat(anulada.get("estado").asString()).isEqualTo("ANULADA");
    }

    @Test
    void conUnPagoRegistradoYaNoSePuedeAnular() throws Exception {
        UUID planilla = planillaCerrada();
        mvcPost(e.rrhh(), PLANILLAS + "/" + planilla + "/pago",
                Map.of("fechaPago", "2030-04-02", "referencia", "OP-12345")).andExpect(status().isCreated());

        mvcPost(admin, PLANILLAS + "/" + planilla + "/anular", Map.of("motivo", "Ya no corresponde"))
                .andExpect(status().isConflict());
        assertThat(leer(admin, PLANILLAS + "/" + planilla)).contains("CERRADA");
    }

    @Test
    void unaPlanillaAnuladaSePuedeRehacer() throws Exception {
        UUID planilla = planillaCerrada();
        mvcPost(admin, PLANILLAS + "/" + planilla + "/anular", Map.of("motivo", "Rehacer")).andExpect(status().isOk());

        mvcPost(e.rrhh(), PLANILLAS, Map.of("periodo", "2030-03", "tipo", "MENSUAL")).andExpect(status().isCreated());
    }

    @Test
    void elPagoSoloSeRegistraSobreUnaPlanillaCerrada() throws Exception {
        UUID borrador = crearPlanilla("2030-03", "MENSUAL");
        mvcPost(e.rrhh(), PLANILLAS + "/" + borrador + "/pago", Map.of("fechaPago", "2030-04-02", "referencia", "X1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anularExigeElMotivo() throws Exception {
        UUID planilla = planillaCerrada();

        mvcPost(admin, PLANILLAS + "/" + planilla + "/anular", Map.of("motivo", " ")).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- gratificación y CTS

    @Test
    void simulaLaGratificacionSinGuardarNada() throws Exception {
        ahoraEs("2030-07-02T10:00");

        JsonNode simulacion = leerJson(mvcPost(e.rrhh(), PLANILLAS + "/gratificacion", Map.of("periodo", "2030-07"))
                .andExpect(status().isOk()));

        assertThat(simulacion.get("tipo").asString()).isEqualTo("GRATIFICACION");
        assertThat(simulacion.get("trabajadores").asInt()).isEqualTo(3);
        assertThat(simulacion.has("id")).isFalse();
        JsonNode deAna = lineaDe(simulacion.get("lineas"), ana);
        assertThat(monto(deAna, "gratificacion")).isEqualByComparingTo("3113.00");
        assertThat(monto(deAna, "bonificacionExtraordinaria")).isEqualByComparingTo("280.17");
        assertThat(monto(deAna, "neto")).isEqualByComparingTo("3393.17");
        // Carla ingresó el 16 de marzo: computa 3 meses completos del semestre (del 16/03 al 15/06).
        assertThat(monto(lineaDe(simulacion.get("lineas"), carla), "gratificacion")).isEqualByComparingTo("1000.00");
        assertThat(json.readTree(leer(e.rrhh(), PLANILLAS + "?periodo=2030-07")).get("totalElementos").asInt())
                .isZero();
    }

    @Test
    void generaLaGratificacionYSeCierraComoCualquierPlanilla() throws Exception {
        ahoraEs("2030-07-02T10:00");

        JsonNode generada = leerJson(mvcPost(e.rrhh(), PLANILLAS + "/gratificacion",
                Map.of("periodo", "2030-07", "simular", false)).andExpect(status().isCreated()));

        assertThat(generada.get("estado").asString()).isEqualTo("CALCULADA");
        UUID planilla = UUID.fromString(generada.get("id").asString());
        JsonNode validacion = json.readTree(leer(e.rrhh(), PLANILLAS + "/" + planilla + "/validaciones"));
        assertThat(validacion.get("valida").asBoolean()).isTrue();
        cerrarCon(e.rrhh(), planilla, "grati-julio").andExpect(status().isOk());
    }

    @Test
    void laGratificacionSoloSePagaEnJulioYDiciembre() throws Exception {
        mvcPost(e.rrhh(), PLANILLAS + "/gratificacion", Map.of("periodo", "2030-03"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void simulaLaCtsDeNoviembreConLaUltimaGratificacionCerrada() throws Exception {
        ahoraEs("2030-07-02T10:00");
        JsonNode generada = leerJson(mvcPost(e.rrhh(), PLANILLAS + "/gratificacion",
                Map.of("periodo", "2030-07", "simular", false)).andExpect(status().isCreated()));
        cerrarCon(e.rrhh(), UUID.fromString(generada.get("id").asString()), "grati-julio").andExpect(status().isOk());
        ahoraEs("2030-11-04T10:00");

        JsonNode simulacion = leerJson(mvcPost(e.rrhh(), PLANILLAS + "/cts", Map.of("periodo", "2030-11"))
                .andExpect(status().isOk()));

        assertThat(simulacion.get("tipo").asString()).isEqualTo("CTS");
        JsonNode deAna = lineaDe(simulacion.get("lineas"), ana);
        // (3000 + 113 + 3113 / 6) / 2 por un semestre completo.
        assertThat(monto(deAna, "cts")).isEqualByComparingTo("1815.92");
        assertThat(monto(deAna, "neto")).isEqualByComparingTo("1815.92");
    }

    // ---------------------------------------------------------------- job

    @Test
    void elJobPrecalculaLaPlanillaElUltimoDiaHabilYNoLaDuplica() throws Exception {
        jdbc.update("update shedlock set lock_until = now() - interval '1 hour'");
        LocalDate ultimoDiaHabil = LocalDate.of(2030, 3, 29);

        assertThat(job.ejecutar(LocalDate.of(2030, 3, 28))).isTrue();
        assertThat(json.readTree(leer(e.rrhh(), PLANILLAS + "?periodo=2030-03")).get("totalElementos").asInt())
                .isZero();

        jdbc.update("update shedlock set lock_until = now() - interval '1 hour'");
        assertThat(job.ejecutar(ultimoDiaHabil)).isTrue();
        JsonNode lista = json.readTree(leer(e.rrhh(), PLANILLAS + "?periodo=2030-03"));
        assertThat(lista.get("totalElementos").asInt()).isEqualTo(1);
        assertThat(lista.get("contenido").get(0).get("estado").asString()).isEqualTo("CALCULADA");

        jdbc.update("update shedlock set lock_until = now() - interval '1 hour'");
        job.ejecutar(ultimoDiaHabil);
        assertThat(json.readTree(leer(e.rrhh(), PLANILLAS + "?periodo=2030-03")).get("totalElementos").asInt())
                .isEqualTo(1);
    }

    // ---------------------------------------------------------------- seguridad

    @Test
    void soloRrhhYAdminDeLaEmpresaGestionanPlanillas() throws Exception {
        Usuario empleado = usuarioDeEmpleado(e, ana);
        Usuario adminSaas = nuevoUsuario(e.empresa(), Rol.ADMIN_SAAS);

        mvc.perform(get(PLANILLAS).header("Authorization", bearer(empleado))).andExpect(status().isForbidden());
        mvc.perform(get(PLANILLAS).header("Authorization", bearer(adminSaas))).andExpect(status().isForbidden());
        mvc.perform(get(PLANILLAS)).andExpect(status().isUnauthorized());
        mvc.perform(get(PLANILLAS).header("Authorization", bearer(e.rrhh()))).andExpect(status().isOk());
        mvc.perform(get(PLANILLAS).header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    void otraEmpresaNoVeLasPlanillasAjenas() throws Exception {
        UUID planilla = crearPlanilla("2030-03", "MENSUAL");
        Escenario otra = nuevoEscenario();

        mvc.perform(get(PLANILLAS + "/" + planilla).header("Authorization", bearer(otra.rrhh())))
                .andExpect(status().isNotFound());
        mvc.perform(post(PLANILLAS + "/" + planilla + "/calcular").header("Authorization", bearer(otra.rrhh())))
                .andExpect(status().isNotFound());
        assertThat(json.readTree(leer(otra.rrhh(), PLANILLAS)).get("totalElementos").asInt()).isZero();
    }
}
