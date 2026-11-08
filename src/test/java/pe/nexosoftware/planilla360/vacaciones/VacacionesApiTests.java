package pe.nexosoftware.planilla360.vacaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.PersonalBase;
import pe.nexosoftware.planilla360.soporte.RucDePrueba;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.scheduling.EstadosVacacionesJob;
import tools.jackson.databind.JsonNode;

/** Saldo, flujo de aprobación (jefe -> RRHH), evento hacia asistencia, permisos y venta de días. */
class VacacionesApiTests extends PersonalBase {

    private static final String SOLICITUDES = "/api/v1/vacaciones/solicitudes";

    @Autowired
    private EstadosVacacionesJob job;

    @Autowired
    private JdbcTemplate jdbc;

    private Escenario a;
    private UUID jefeId;
    private UUID empleadoId;
    private Usuario jefe;
    private Usuario empleado;

    @BeforeEach
    void escenario() throws Exception {
        jdbc.update("update shedlock set lock_until = now() - interval '1 hour'");
        ahoraEs("2030-03-05T10:00");
        a = nuevoEscenario();
        jefeId = crearEmpleado(a, "Mario", "Jefe de Planta", "2020-01-01");
        empleadoId = crearEmpleadoConJefe(a, "Lucía", "2029-03-01", jefeId);
        jefe = usuarioDeEmpleado(a, jefeId);
        empleado = usuarioDeEmpleado(a, empleadoId);
        asignarHorarioOficina(a, empleadoId);
    }

    private void asignarHorarioOficina(Escenario e, UUID empleado) throws Exception {
        UUID horario = crearId(e.rrhh(), "/api/v1/horarios", Map.of(
                "nombre", "Oficina", "entrada", "08:00", "salida", "17:00", "toleranciaMin", 10,
                "dias", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")));
        mvc.perform(post("/api/v1/horarios/" + horario + "/asignaciones").header("Authorization", bearer(e.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("empleadoId", empleado.toString(), "desde", "2029-03-01"))))
                .andExpect(status().isNoContent());
    }

    private String solicitar(Usuario como, String desde, String hasta) throws Exception {
        String respuesta = mvc.perform(post(SOLICITUDES).header("Authorization", bearer(como))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"" + desde + "\",\"hasta\":\"" + hasta + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asString();
    }

    private void accion(Usuario como, String id, String accion, int estadoEsperado) throws Exception {
        mvc.perform(post(SOLICITUDES + "/" + id + "/" + accion).header("Authorization", bearer(como))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Cierre de campaña\"}"))
                .andExpect(status().is(estadoEsperado));
    }

    private JsonNode saldo(Usuario como) throws Exception {
        return json.readTree(leer(como, "/api/v1/vacaciones/saldo/mio"));
    }

    private void correrJob() {
        jdbc.update("update shedlock set lock_until = now() - interval '1 hour'");
        assertThat(job.ejecutar()).isTrue();
    }

    @Test
    void elSaldoSeCalculaPorAniosDeServicioCumplidos() throws Exception {
        JsonNode saldo = saldo(empleado);

        assertThat(saldo.get("diasDisponibles").asInt()).isEqualTo(30);
        assertThat(saldo.get("periodos")).hasSize(1);
        assertThat(saldo.get("periodos").get(0).get("desde").asString()).isEqualTo("2029-03-01");
        assertThat(saldo.get("periodos").get(0).get("hasta").asString()).isEqualTo("2030-02-28");

        // Un empleado con menos de un año todavía no ganó vacaciones.
        UUID nuevo = crearEmpleado(a, "Reciente", "Ingreso", "2029-12-01");
        assertThat(saldo(usuarioDeEmpleado(a, nuevo)).get("diasDisponibles").asInt()).isZero();
    }

    @Test
    void aprobacionCompletaPublicaElEventoYAsistenciaJustificaLosDias() throws Exception {
        String id = solicitar(empleado, "2030-04-10", "2030-04-16");
        JsonNode reservado = saldo(empleado);
        assertThat(reservado.get("diasDisponibles").asInt()).isEqualTo(23);
        assertThat(reservado.get("periodos").get(0).get("diasReservados").asInt()).isEqualTo(7);

        accion(empleado, id, "aprobar-jefe", 403);
        accion(a.rrhh(), id, "aprobar-rrhh", 422);
        accion(usuarioDeEmpleado(a, crearEmpleado(a, "Otro", "Colega", "2023-01-01")), id, "aprobar-jefe", 403);
        accion(jefe, id, "aprobar-jefe", 200);
        accion(jefe, id, "aprobar-rrhh", 403);
        accion(a.rrhh(), id, "aprobar-rrhh", 200);

        ahoraEs("2030-04-11T00:30");
        mvc.perform(post("/api/v1/asistencia/consolidaciones").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fecha\":\"2030-04-10\"}"))
                .andExpect(status().isOk());
        JsonNode dia = json.readTree(leer(empleado, "/api/v1/asistencia/mia?mes=2030-04")).get(0);
        assertThat(dia.get("fecha").asString()).isEqualTo("2030-04-10");
        assertThat(dia.get("estado").asString()).isEqualTo("JUSTIFICADA");
    }

    @Test
    void conElPasoDeLosDiasElGoceEmpiezaYTerminaYElSaldoSeDescuenta() throws Exception {
        String id = solicitar(empleado, "2030-04-10", "2030-04-16");
        accion(jefe, id, "aprobar-jefe", 200);
        accion(a.rrhh(), id, "aprobar-rrhh", 200);

        ahoraEs("2030-04-09T10:00");
        correrJob();
        assertThat(estado(empleado, id)).isEqualTo("APROBADA_RRHH");

        ahoraEs("2030-04-10T10:00");
        correrJob();
        assertThat(estado(empleado, id)).isEqualTo("EN_GOCE");

        ahoraEs("2030-04-17T10:00");
        correrJob();
        assertThat(estado(empleado, id)).isEqualTo("FINALIZADA");
        JsonNode periodo = saldo(empleado).get("periodos").get(0);
        assertThat(periodo.get("diasGozados").asInt()).isEqualTo(7);
        assertThat(periodo.get("diasReservados").asInt()).isZero();
        assertThat(periodo.get("diasDisponibles").asInt()).isEqualTo(23);
    }

    private String estado(Usuario como, String id) throws Exception {
        for (JsonNode s : json.readTree(leer(como, SOLICITUDES))) {
            if (s.get("id").asString().equals(id)) {
                return s.get("estado").asString();
            }
        }
        throw new AssertionError("La solicitud no aparece en el listado");
    }

    @Test
    void elRechazoLiberaLosDiasYGuardaElMotivo() throws Exception {
        String id = solicitar(empleado, "2030-04-10", "2030-04-24");
        assertThat(saldo(empleado).get("diasDisponibles").asInt()).isEqualTo(15);

        accion(empleado, id, "rechazar", 403);
        accion(jefe, id, "rechazar", 200);

        assertThat(saldo(empleado).get("diasDisponibles").asInt()).isEqualTo(30);
        mvc.perform(post(SOLICITUDES + "/" + id + "/aprobar-jefe").header("Authorization", bearer(jefe)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(json.readTree(leer(empleado, SOLICITUDES)).get(0).get("motivoRechazo").asString())
                .isEqualTo("Cierre de campaña");
    }

    @Test
    void siElEmpleadoNoTieneJefeRrhhHaceLaPrimeraAprobacion() throws Exception {
        UUID sinJefe = crearEmpleado(a, "Sin", "Jefe", "2029-03-01");
        Usuario usuario = usuarioDeEmpleado(a, sinJefe);
        String id = solicitar(usuario, "2030-05-06", "2030-05-12");

        accion(a.rrhh(), id, "aprobar-jefe", 200);
        accion(a.rrhh(), id, "aprobar-rrhh", 200);
    }

    @Test
    void reglasDeNegocioDeLaSolicitud() throws Exception {
        String primera = solicitar(empleado, "2030-04-10", "2030-04-16");

        // Solape con la solicitud activa.
        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-04-15\",\"hasta\":\"2030-04-30\"}"))
                .andExpect(status().isConflict());
        // Bloque menor a 7 días.
        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-06-03\",\"hasta\":\"2030-06-07\"}"))
                .andExpect(status().isUnprocessableEntity());
        // Más días que el saldo disponible (23).
        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-07-01\",\"hasta\":\"2030-07-30\"}"))
                .andExpect(status().isUnprocessableEntity());
        // En el pasado y con fechas invertidas.
        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-03-01\",\"hasta\":\"2030-03-10\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-08-10\",\"hasta\":\"2030-08-01\"}"))
                .andExpect(status().isUnprocessableEntity());
        // Una cuenta sin empleado no puede solicitar.
        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(nuevoUsuario(a.empresa(), pe.nexosoftware.planilla360.compartido.seguridad.Rol.EMPLEADO)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-09-02\",\"hasta\":\"2030-09-08\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(primera).isNotBlank();
    }

    @Test
    void sinSaldoNoSePuedeSolicitar() throws Exception {
        UUID reciente = crearEmpleado(a, "Reciente", "Ingreso", "2029-12-01");

        mvc.perform(post(SOLICITUDES).header("Authorization", bearer(usuarioDeEmpleado(a, reciente)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2030-09-02\",\"hasta\":\"2030-09-08\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelarSoloHastaLaAprobacionDelJefe() throws Exception {
        String propia = solicitar(empleado, "2030-04-10", "2030-04-16");
        accion(usuarioDeEmpleado(a, crearEmpleado(a, "Ajeno", "Colega", "2023-01-01")), propia, "cancelar", 403);
        accion(empleado, propia, "cancelar", 200);
        assertThat(saldo(empleado).get("diasDisponibles").asInt()).isEqualTo(30);

        String aprobada = solicitar(empleado, "2030-05-06", "2030-05-12");
        accion(jefe, aprobada, "aprobar-jefe", 200);
        accion(a.rrhh(), aprobada, "aprobar-rrhh", 200);
        accion(empleado, aprobada, "cancelar", 422);
    }

    @Test
    void cadaQuienVeLoSuyoYLoDeSuEquipo() throws Exception {
        solicitar(empleado, "2030-04-10", "2030-04-16");
        Usuario colega = usuarioDeEmpleado(a, crearEmpleado(a, "Ajeno", "Colega", "2023-01-01"));

        assertThat(json.readTree(leer(empleado, SOLICITUDES))).hasSize(1);
        assertThat(json.readTree(leer(jefe, SOLICITUDES))).hasSize(1);
        assertThat(json.readTree(leer(colega, SOLICITUDES))).isEmpty();
        assertThat(json.readTree(leer(a.rrhh(), SOLICITUDES + "?estado=SOLICITADA"))).hasSize(1);
        assertThat(json.readTree(leer(a.rrhh(), SOLICITUDES + "?estado=RECHAZADA"))).isEmpty();
    }

    @Test
    void otraEmpresaNoPuedeVerNiResolverLaSolicitud() throws Exception {
        String id = solicitar(empleado, "2030-04-10", "2030-04-16");
        Escenario b = nuevoEscenario();

        accion(b.rrhh(), id, "aprobar-jefe", 404);
        accion(b.rrhh(), id, "aprobar-rrhh", 404);
        assertThat(json.readTree(leer(b.rrhh(), SOLICITUDES))).isEmpty();
    }

    @Test
    void elPermisoAprobadoJustificaLosDiasYSoloRrhhLoResuelve() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/permisos").header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "tipo", "MEDICO", "desde", "2030-03-20", "hasta", "2030-03-21", "conGoce", true,
                                "motivo", "Descanso médico"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SOLICITADO"))
                .andExpect(jsonPath("$.dias").value(2))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(respuesta).get("id").asString();

        mvc.perform(post("/api/v1/permisos/" + id + "/aprobar").header("Authorization", bearer(empleado)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/permisos/" + id + "/aprobar").header("Authorization", bearer(jefe)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/permisos/" + id + "/aprobar").header("Authorization", bearer(a.rrhh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
        mvc.perform(post("/api/v1/permisos/" + id + "/rechazar").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"tarde\"}"))
                .andExpect(status().isUnprocessableEntity());

        ahoraEs("2030-03-22T00:30");
        mvc.perform(post("/api/v1/asistencia/consolidaciones").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fecha\":\"2030-03-20\"}"))
                .andExpect(status().isOk());
        assertThat(json.readTree(leer(empleado, "/api/v1/asistencia/mia?mes=2030-03")).get(0).get("estado").asString())
                .isEqualTo("JUSTIFICADA");
    }

    @Test
    void unPermisoRechazadoNoJustificaNada() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/permisos").header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "tipo", "PERSONAL", "desde", "2030-03-20", "hasta", "2030-03-20", "conGoce", false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(respuesta).get("id").asString();

        mvc.perform(post("/api/v1/permisos/" + id + "/rechazar").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Sin sustento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoRechazo").value("Sin sustento"));

        ahoraEs("2030-03-22T00:30");
        mvc.perform(post("/api/v1/asistencia/consolidaciones").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fecha\":\"2030-03-20\"}"))
                .andExpect(status().isOk());
        assertThat(json.readTree(leer(empleado, "/api/v1/asistencia/mia?mes=2030-03")).get(0).get("estado").asString())
                .isEqualTo("FALTA");
    }

    @Test
    void ventaDeDiasSoloEnRegimenGeneralYHastaQuince() throws Exception {
        String periodoId = saldo(empleado).get("periodos").get(0).get("periodoId").asString();
        String ruta = "/api/v1/vacaciones/periodos/" + periodoId + "/venta";

        mvc.perform(post(ruta).header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":5}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":16}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":10}"))
                .andExpect(status().isNoContent());
        assertThat(saldo(empleado).get("diasDisponibles").asInt()).isEqualTo(20);
        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":6}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void laPequenaEmpresaTiene15DiasYNoPuedeVender() throws Exception {
        Empresa pequena = empresas.save(new Empresa(
                RucDePrueba.nuevo(), "Panadería de prueba", RegimenLaboral.PEQUENA_EMPRESA, PlanSaas.BASICO));
        Escenario b = nuevoEscenario(pequena);
        UUID id = crearEmpleado(b, "Panadero", "Maestro", "2029-03-01");
        Usuario usuario = usuarioDeEmpleado(b, id);

        JsonNode saldo = saldo(usuario);
        assertThat(saldo.get("diasDisponibles").asInt()).isEqualTo(15);
        String periodoId = saldo.get("periodos").get(0).get("periodoId").asString();
        mvc.perform(post("/api/v1/vacaciones/periodos/" + periodoId + "/venta").header("Authorization", bearer(b.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":5}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
