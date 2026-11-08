package pe.nexosoftware.planilla360.asistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.PersonalBase;
import tools.jackson.databind.JsonNode;

/** Marcaciones, consolidación, resumen y horas extra de punta a punta (martes 12 de marzo de 2030). */
class AsistenciaApiTests extends PersonalBase {

    private Escenario a;
    private UUID empleadoId;
    private Usuario empleado;

    @BeforeEach
    void escenario() throws Exception {
        a = nuevoEscenario();
        empleadoId = crearEmpleado(a, "Lucía", "Flores Paredes", "2024-01-01");
        empleado = usuarioDeEmpleado(a, empleadoId);
        UUID horario = crearId(a.rrhh(), "/api/v1/horarios", Map.of(
                "nombre", "Oficina", "entrada", "08:00", "salida", "17:00", "toleranciaMin", 10,
                "dias", java.util.List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")));
        mvc.perform(post("/api/v1/horarios/" + horario + "/asignaciones").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("empleadoId", empleadoId.toString(), "desde", "2024-01-01"))))
                .andExpect(status().isNoContent());
    }

    private JsonNode marcar(String ahora) throws Exception {
        ahoraEs(ahora);
        String respuesta = mvc.perform(post("/api/v1/asistencia/marcaciones").header("Authorization", bearer(empleado)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private void consolidar(String ahora, String fecha) throws Exception {
        ahoraEs(ahora);
        mvc.perform(post("/api/v1/asistencia/consolidaciones").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fecha\":\"" + fecha + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode asistenciaDelMes(String ahora) throws Exception {
        ahoraEs(ahora);
        return json.readTree(leer(empleado, "/api/v1/asistencia/mia?mes=2030-03"));
    }

    @Test
    void lasMarcacionesAlternanEntradaYSalidaYSeConsolidanConTardanzaYHorasExtra() throws Exception {
        assertThat(marcar("2030-03-12T08:25").get("tipo").asString()).isEqualTo("ENTRADA");
        assertThat(marcar("2030-03-12T13:00").get("tipo").asString()).isEqualTo("SALIDA");
        assertThat(marcar("2030-03-12T14:00").get("tipo").asString()).isEqualTo("ENTRADA");
        assertThat(marcar("2030-03-12T18:30").get("tipo").asString()).isEqualTo("SALIDA");

        assertThat(asistenciaDelMes("2030-03-12T19:00")).isEmpty();

        consolidar("2030-03-13T00:30", "2030-03-12");

        JsonNode dias = asistenciaDelMes("2030-03-13T09:00");
        assertThat(dias).hasSize(1);
        assertThat(dias.get(0).get("fecha").asString()).isEqualTo("2030-03-12");
        assertThat(dias.get(0).get("estado").asString()).isEqualTo("TARDANZA");
        assertThat(dias.get(0).get("minutosTardanza").asInt()).isEqualTo(25);
        assertThat(dias.get(0).get("minutosExtra").asInt()).isEqualTo(90);
    }

    @Test
    void consolidarDosVecesNoDuplicaElDia() throws Exception {
        marcar("2030-03-12T08:00");
        marcar("2030-03-12T17:00");

        consolidar("2030-03-13T00:30", "2030-03-12");
        consolidar("2030-03-13T00:45", "2030-03-12");

        JsonNode dias = asistenciaDelMes("2030-03-13T09:00");
        assertThat(dias).hasSize(1);
        assertThat(dias.get(0).get("estado").asString()).isEqualTo("PRESENTE");
    }

    @Test
    void unDiaSinMarcacionesEsFaltaYElSabadoEsDescanso() throws Exception {
        consolidar("2030-03-17T00:30", "2030-03-13");
        consolidar("2030-03-17T00:31", "2030-03-16");

        JsonNode dias = asistenciaDelMes("2030-03-17T09:00");
        assertThat(dias).hasSize(2);
        assertThat(dias.get(0).get("estado").asString()).isEqualTo("FALTA");
        assertThat(dias.get(1).get("estado").asString()).isEqualTo("DESCANSO");
    }

    @Test
    void noSePuedeMarcarDosVecesSeguidasNiConsolidarUnDiaQueNoTermino() throws Exception {
        marcar("2030-03-12T08:00");

        ahoraEs("2030-03-12T08:00");
        mvc.perform(post("/api/v1/asistencia/marcaciones").header("Authorization", bearer(empleado)))
                .andExpect(status().isUnprocessableEntity());

        ahoraEs("2030-03-12T12:00");
        mvc.perform(post("/api/v1/asistencia/consolidaciones").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fecha\":\"2030-03-12\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unaCuentaSinEmpleadoNoPuedeMarcar() throws Exception {
        Usuario sinEmpleado = nuevoUsuario(a.empresa(), Rol.EMPLEADO);

        mvc.perform(post("/api/v1/asistencia/marcaciones").header("Authorization", bearer(sinEmpleado)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void elResumenMensualTotalizaTardanzasFaltasYMinutosExtra() throws Exception {
        marcar("2030-03-12T08:30");
        marcar("2030-03-12T17:30");
        consolidar("2030-03-14T00:30", "2030-03-12");
        consolidar("2030-03-14T00:31", "2030-03-13");

        ahoraEs("2030-03-14T09:00");
        JsonNode resumen = json.readTree(leer(a.rrhh(), "/api/v1/asistencia/resumen?mes=2030-03"));

        assertThat(resumen).hasSize(1);
        JsonNode fila = resumen.get(0);
        assertThat(fila.get("empleadoId").asString()).isEqualTo(empleadoId.toString());
        assertThat(fila.get("nombreCompleto").asString()).contains("Flores Paredes");
        assertThat(fila.get("diasAsistidos").asInt()).isEqualTo(1);
        assertThat(fila.get("tardanzas").asInt()).isEqualTo(1);
        assertThat(fila.get("minutosTardanza").asInt()).isEqualTo(30);
        assertThat(fila.get("faltas").asInt()).isEqualTo(1);
        assertThat(fila.get("minutosExtra").asInt()).isEqualTo(30);
    }

    @Test
    void lasMarcacionesNoSeVenEntreEmpresasYLosRolesSeRespetan() throws Exception {
        marcar("2030-03-12T08:00");
        marcar("2030-03-12T17:00");
        consolidar("2030-03-13T00:30", "2030-03-12");
        Escenario b = nuevoEscenario();

        mvc.perform(get("/api/v1/asistencia?empleadoId=" + empleadoId + "&mes=2030-03")
                        .header("Authorization", bearer(b.rrhh())))
                .andExpect(status().isNotFound());
        assertThat(json.readTree(leer(b.rrhh(), "/api/v1/asistencia/resumen?mes=2030-03"))).isEmpty();

        mvc.perform(get("/api/v1/asistencia?empleadoId=" + empleadoId + "&mes=2030-03")
                        .header("Authorization", bearer(empleado)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/horarios").header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"entrada\":\"08:00\",\"salida\":\"17:00\",\"toleranciaMin\":5,\"dias\":[\"MONDAY\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/asistencia/mia?mes=2030-03")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/asistencia?empleadoId=" + empleadoId + "&mes=2030-03")
                        .header("Authorization", bearer(a.rrhh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PRESENTE"));
    }

    @Test
    void unHorarioPuedeTrabajarLos7DiasDeLaSemana() throws Exception {
        mvc.perform(post("/api/v1/horarios").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Guardia\",\"entrada\":\"06:00\",\"salida\":\"14:00\",\"toleranciaMin\":5,"
                                + "\"dias\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dias.length()").value(7));
    }

    @Test
    void unHorarioInvalidoOSuNombreRepetidoSeRechazan() throws Exception {
        String sinTolerancia = "{\"nombre\":\"Turno\",\"entrada\":\"17:00\",\"salida\":\"08:00\",\"toleranciaMin\":5,\"dias\":[\"MONDAY\"]}";
        mvc.perform(post("/api/v1/horarios").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(sinTolerancia))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/v1/horarios").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"oficina\",\"entrada\":\"08:00\",\"salida\":\"17:00\",\"toleranciaMin\":5,\"dias\":[\"MONDAY\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void lasHorasExtraSeSolicitanYLasApruebaElJefeDirectoNuncaElMismoSolicitante() throws Exception {
        UUID jefeId = crearEmpleado(a, "Mario", "Jefe de Planta", "2020-01-01");
        UUID subordinadoId = crearEmpleadoConJefe(a, "Sara", "2023-01-01", jefeId);
        Usuario jefe = usuarioDeEmpleado(a, jefeId);
        Usuario subordinado = usuarioDeEmpleado(a, subordinadoId);
        Usuario otroEmpleado = usuarioDeEmpleado(a, crearEmpleado(a, "Otro", "Colega", "2023-01-01"));
        ahoraEs("2030-03-13T10:00");

        String respuesta = mvc.perform(post("/api/v1/asistencia/horas-extra").header("Authorization", bearer(subordinado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fecha", "2030-03-12", "horas", "2.5", "motivo", "Cierre de lote"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.empleadoId").value(subordinadoId.toString()))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(respuesta).get("id").asString();
        String resolver = "{\"accion\":\"APROBAR\"}";

        mvc.perform(patch("/api/v1/asistencia/horas-extra/" + id).header("Authorization", bearer(subordinado))
                        .contentType(MediaType.APPLICATION_JSON).content(resolver))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/asistencia/horas-extra/" + id).header("Authorization", bearer(otroEmpleado))
                        .contentType(MediaType.APPLICATION_JSON).content(resolver))
                .andExpect(status().isForbidden());

        assertThat(json.readTree(leer(jefe, "/api/v1/asistencia/horas-extra?estado=PENDIENTE"))).hasSize(1);
        assertThat(json.readTree(leer(otroEmpleado, "/api/v1/asistencia/horas-extra"))).isEmpty();

        mvc.perform(patch("/api/v1/asistencia/horas-extra/" + id).header("Authorization", bearer(jefe))
                        .contentType(MediaType.APPLICATION_JSON).content(resolver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.aprobadoPor").value(jefe.getId().toString()));
        mvc.perform(patch("/api/v1/asistencia/horas-extra/" + id).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"RECHAZAR\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(json.readTree(leer(a.rrhh(), "/api/v1/asistencia/horas-extra?estado=APROBADA"))).hasSize(1);
    }

    @Test
    void unEmpleadoNoSolicitaHorasExtraParaOtroPeroRrhhSiLoRegistra() throws Exception {
        UUID colega = crearEmpleado(a, "Otro", "Colega", "2023-01-01");
        ahoraEs("2030-03-13T10:00");
        String cuerpo = json.writeValueAsString(Map.of(
                "empleadoId", colega.toString(), "fecha", "2030-03-12", "horas", "1", "motivo", "Apoyo"));

        mvc.perform(post("/api/v1/asistencia/horas-extra").header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/asistencia/horas-extra").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/asistencia/horas-extra").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2030-03-12\",\"horas\":\"1\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/asistencia/horas-extra").header("Authorization", bearer(empleado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2030-03-12\",\"horas\":\"9\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
