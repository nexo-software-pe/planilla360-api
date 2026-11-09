package pe.nexosoftware.planilla360.empleado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.PersonalBase;
import tools.jackson.databind.JsonNode;

/** CRUD de empleados, búsqueda con Specification, contratos, familiares, roles y aislamiento por empresa. */
class EmpleadoApiTests extends PersonalBase {

    private Escenario a;

    @BeforeEach
    void escenario() throws Exception {
        a = nuevoEscenario();
    }

    private String texto(Object cuerpo) {
        return json.writeValueAsString(cuerpo);
    }

    private JsonNode listar(String consulta) throws Exception {
        return json.readTree(leer(a.rrhh(), "/api/v1/empleados" + consulta));
    }

    @Test
    void rrhhCreaConsultaYActualizaUnEmpleado() throws Exception {
        UUID id = crearEmpleado(a, "Ana María", "Quispe Rojas", "2024-03-01");

        mvc.perform(get("/api/v1/empleados/" + id).header("Authorization", bearer(a.rrhh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Ana María"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaCese").doesNotExist());

        Map<String, Object> cuerpo = new LinkedHashMap<>(cuerpoEmpleado(a, "Ana María", "Quispe Rojas", "2024-03-01"));
        cuerpo.put("dni", json.readTree(leer(a.rrhh(), "/api/v1/empleados/" + id)).get("dni").asString());
        cuerpo.put("telefono", "987654321");
        mvc.perform(put("/api/v1/empleados/" + id).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("987654321"));
    }

    @Test
    void elDniNoSePuedeRepetirEnLaEmpresaPeroSiEnOtra() throws Exception {
        Map<String, Object> cuerpo = cuerpoEmpleado(a, "Luis", "Ramos", "2024-01-15");
        crearId(a.rrhh(), "/api/v1/empleados", cuerpo);

        mvc.perform(post("/api/v1/empleados").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpo)))
                .andExpect(status().isConflict());

        Escenario b = nuevoEscenario();
        Map<String, Object> enB = cuerpoEmpleado(b, "Luis", "Ramos", "2024-01-15");
        enB.put("dni", cuerpo.get("dni"));
        crearId(b.rrhh(), "/api/v1/empleados", enB);
    }

    @Test
    void validaDatosDeEntradaYReglasDeNegocio() throws Exception {
        Map<String, Object> dniMalo = cuerpoEmpleado(a, "Luis", "Ramos", "2024-01-15");
        dniMalo.put("dni", "123");
        mvc.perform(post("/api/v1/empleados").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(dniMalo)))
                .andExpect(status().isBadRequest());

        Map<String, Object> menor = cuerpoEmpleado(a, "Luis", "Ramos", "2024-01-15");
        menor.put("fechaNacimiento", "2012-01-01");
        mvc.perform(post("/api/v1/empleados").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(menor)))
                .andExpect(status().isUnprocessableEntity());

        Map<String, Object> areaAjena = cuerpoEmpleado(a, "Luis", "Ramos", "2024-01-15");
        areaAjena.put("areaId", UUID.randomUUID().toString());
        mvc.perform(post("/api/v1/empleados").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(areaAjena)))
                .andExpect(status().isNotFound());
    }

    @Test
    void laBusquedaCombinaCriteriosConSpecification() throws Exception {
        crearEmpleado(a, "Carmen", "Flores Díaz", "2023-02-01");
        crearEmpleado(a, "Carlos", "Mendoza Vega", "2024-06-01");
        crearEmpleado(a, "Rosa", "Huamán", "2025-01-10");

        assertThat(listar("").get("totalElementos").asInt()).isEqualTo(3);
        assertThat(listar("?q=car").get("totalElementos").asInt()).isEqualTo(2);
        assertThat(listar("?q=CARMEN flores").get("totalElementos").asInt()).isEqualTo(1);
        assertThat(listar("?q=%").get("totalElementos").asInt()).isZero();
        assertThat(listar("?ingresoDesde=2024-01-01").get("totalElementos").asInt()).isEqualTo(2);
        assertThat(listar("?ingresoDesde=2024-01-01&ingresoHasta=2024-12-31&q=car").get("totalElementos").asInt())
                .isEqualTo(1);
        assertThat(listar("?areaId=" + a.areaId() + "&estado=ACTIVO").get("totalElementos").asInt()).isEqualTo(3);
        assertThat(listar("?estado=CESADO").get("totalElementos").asInt()).isZero();
        assertThat(listar("?areaId=" + UUID.randomUUID()).get("totalElementos").asInt()).isZero();

        String dni = json.readTree(listar("?q=huam").get("contenido").get(0).toString()).get("dni").asString();
        assertThat(listar("?q=" + dni).get("totalElementos").asInt()).isEqualTo(1);
    }

    @Test
    void laBusquedaPaginaYOrdena() throws Exception {
        crearEmpleado(a, "Beto", "Zamora", "2024-01-01");
        crearEmpleado(a, "Alba", "Acosta", "2024-01-01");
        crearEmpleado(a, "Cira", "Mora", "2024-01-01");

        JsonNode pagina = listar("?size=2&sort=apellidos,desc");
        assertThat(pagina.get("totalElementos").asInt()).isEqualTo(3);
        assertThat(pagina.get("totalPaginas").asInt()).isEqualTo(2);
        assertThat(pagina.get("contenido").get(0).get("apellidos").asString()).isEqualTo("Zamora");

        mvc.perform(get("/api/v1/empleados?sort=campoInexistente").header("Authorization", bearer(a.rrhh())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaEmpresaNoVeNiTocaEmpleadosDeOtra() throws Exception {
        UUID idDeA = crearEmpleado(a, "Ana", "Solo de A", "2024-01-01");
        Escenario b = nuevoEscenario();
        crearEmpleado(b, "Berta", "Solo de B", "2024-01-01");

        assertThat(json.readTree(leer(b.rrhh(), "/api/v1/empleados")).get("totalElementos").asInt()).isEqualTo(1);
        assertThat(listar("?q=Berta").get("totalElementos").asInt()).isZero();
        mvc.perform(get("/api/v1/empleados/" + idDeA).header("Authorization", bearer(b.rrhh())))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/empleados/" + idDeA + "/cese").header("Authorization", bearer(b.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaCese\":\"2025-01-01\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/empleados/" + idDeA + "/contratos").header("Authorization", bearer(b.rrhh())))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloRrhhYAdminDeEmpresaAccedenAlPersonal() throws Exception {
        UUID id = crearEmpleado(a, "Ana", "Prueba", "2024-01-01");
        Usuario empleado = usuarioDeEmpleado(a, id);
        Usuario admin = nuevoUsuario(a.empresa(), Rol.ADMIN_EMPRESA);
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);

        mvc.perform(get("/api/v1/empleados")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/empleados").header("Authorization", bearer(empleado)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/empleados").header("Authorization", bearer(saas)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/empleados").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    void laJerarquiaDeJefesNoPermiteAutoreferenciaNiCiclos() throws Exception {
        UUID director = crearEmpleado(a, "Director", "General", "2020-01-01");
        UUID gerente = crearEmpleadoConJefe(a, "Gerente", "2021-01-01", director);
        UUID analista = crearEmpleadoConJefe(a, "Analista", "2022-01-01", gerente);

        Map<String, Object> directorConJefe = cuerpoEmpleado(a, "Director", "General", "2020-01-01");
        directorConJefe.put("dni", json.readTree(leer(a.rrhh(), "/api/v1/empleados/" + director)).get("dni").asString());
        directorConJefe.put("jefeId", analista.toString());
        mvc.perform(put("/api/v1/empleados/" + director).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(directorConJefe)))
                .andExpect(status().isUnprocessableEntity());

        directorConJefe.put("jefeId", director.toString());
        mvc.perform(put("/api/v1/empleados/" + director).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(directorConJefe)))
                .andExpect(status().isUnprocessableEntity());

        assertThat(listar("?jefeId=" + director).get("totalElementos").asInt()).isEqualTo(1);
    }

    @Test
    void elCeseFijaEstadoYFechaYBloqueaNuevasModificaciones() throws Exception {
        UUID id = crearEmpleado(a, "Ana", "Prueba", "2024-01-01");

        mvc.perform(patch("/api/v1/empleados/" + id + "/cese").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaCese\":\"2023-12-31\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(patch("/api/v1/empleados/" + id + "/cese").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaCese\":\"2025-06-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CESADO"))
                .andExpect(jsonPath("$.fechaCese").value("2025-06-30"));
        mvc.perform(patch("/api/v1/empleados/" + id + "/cese").header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaCese\":\"2025-07-30\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void contratosConRegimenPensionarioSueldoMinimoYEncadenamiento() throws Exception {
        UUID id = crearEmpleado(a, "Ana", "Prueba", "2024-01-01");
        String ruta = "/api/v1/empleados/" + id + "/contratos";
        UUID afp = UUID.fromString(json.readTree(leer(a.rrhh(), "/api/v1/afps")).get(0).get("id").asString());

        Map<String, Object> onp = contrato("INDEFINIDO", "2024-01-01", "1800.00", "ONP");
        Map<String, Object> afpSinDatos = contrato("INDEFINIDO", "2025-01-01", "1800.00", "AFP");
        Map<String, Object> onpConAfp = contrato("INDEFINIDO", "2025-01-01", "1800.00", "ONP");
        onpConAfp.put("afpId", afp.toString());
        Map<String, Object> bajoRmv = contrato("INDEFINIDO", "2025-01-01", "1000.00", "ONP");
        Map<String, Object> plazoSinFin = contrato("PLAZO_FIJO", "2025-01-01", "1800.00", "ONP");
        Map<String, Object> antesDelIngreso = contrato("INDEFINIDO", "2023-06-01", "1800.00", "ONP");

        for (Map<String, Object> invalido : java.util.List.of(afpSinDatos, onpConAfp, bajoRmv, plazoSinFin, antesDelIngreso)) {
            mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                            .contentType(MediaType.APPLICATION_JSON).content(texto(invalido)))
                    .andExpect(status().isUnprocessableEntity());
        }

        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(onp)))
                .andExpect(status().isCreated());

        Map<String, Object> conAfp = contrato("INDEFINIDO", "2025-01-01", "2200.00", "AFP");
        conAfp.put("afpId", afp.toString());
        conAfp.put("tipoComision", "FLUJO");
        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(conAfp)))
                .andExpect(status().isCreated());

        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(onp)))
                .andExpect(status().isUnprocessableEntity());

        JsonNode historial = json.readTree(leer(a.rrhh(), ruta));
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).get("fechaInicio").asString()).isEqualTo("2025-01-01");
        assertThat(historial.get(0).get("fechaFin").isNull()).isTrue();
        assertThat(historial.get(1).get("fechaFin").asString()).isEqualTo("2024-12-31");
    }

    @Test
    void sueldoMinimoDeTiempoParcialEsProporcionalALaJornada() throws Exception {
        String ruta23 = "/api/v1/empleados/" + crearEmpleado(a, "Pía", "Parcial", "2024-01-01") + "/contratos";
        String ruta24 = "/api/v1/empleados/" + crearEmpleado(a, "Raúl", "Medio", "2024-01-01") + "/contratos";

        // 23 h: RMV × 23 / 48 = 541.46 (1 130 vigente)
        enviarContrato(ruta23, "PART_TIME", "541.45", 23).andExpect(status().isUnprocessableEntity());
        enviarContrato(ruta23, "PART_TIME", "541.46", 23).andExpect(status().isCreated());

        // 24 h: ya se exige la RMV completa aunque el contrato sea part-time
        enviarContrato(ruta24, "PART_TIME", "900.00", 24).andExpect(status().isUnprocessableEntity());
        enviarContrato(ruta24, "PART_TIME", "1130.00", 24).andExpect(status().isCreated());
    }

    @Test
    void losDiasLaborablesDelContratoSeGuardanYPorDefectoSonCinco() throws Exception {
        UUID id = crearEmpleado(a, "Ana", "Seis Dias", "2024-01-01");
        String ruta = "/api/v1/empleados/" + id + "/contratos";

        enviarContrato(ruta, "INDEFINIDO", "2000.00", 48).andExpect(status().isCreated())
                .andExpect(jsonPath("$.diasLaborables").value(5));

        Map<String, Object> seisDias = contrato("INDEFINIDO", "2025-06-01", "2000.00", "ONP");
        seisDias.put("diasLaborables", 6);
        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(seisDias)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diasLaborables").value(6));

        seisDias.put("diasLaborables", 8);
        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(seisDias)))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions enviarContrato(
            String ruta, String tipo, String sueldo, int jornada) throws Exception {
        Map<String, Object> cuerpo = contrato(tipo, "2025-01-01", sueldo, "ONP");
        cuerpo.put("jornadaSemanal", jornada);
        return mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpo)));
    }

    @Test
    void familiaresSeRegistranYListan() throws Exception {
        UUID id = crearEmpleado(a, "Ana", "Prueba", "2024-01-01");
        String ruta = "/api/v1/empleados/" + id + "/familiares";

        mvc.perform(post(ruta).header("Authorization", bearer(a.rrhh()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(texto(Map.of("parentesco", "HIJO", "nombres", "Mateo", "fechaNacimiento", "2015-04-02", "estudia", true))))
                .andExpect(status().isCreated());

        mvc.perform(get(ruta).header("Authorization", bearer(a.rrhh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombres").value("Mateo"))
                .andExpect(jsonPath("$[0].estudia").value(true));
    }

    @Test
    void losCambiosDeEmpleadosQuedanAuditados() throws Exception {
        UUID id = crearEmpleado(a, "Ana", "Auditada", "2024-01-01");
        Usuario admin = nuevoUsuario(a.empresa(), Rol.ADMIN_EMPRESA);

        mvc.perform(get("/api/v1/auditoria?entidad=EMPLEADO").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].entidadId").value(id.toString()))
                .andExpect(jsonPath("$.contenido[0].accion").value("CREAR"));
    }

    private static Map<String, Object> contrato(String tipo, String inicio, String sueldo, String regimen) {
        Map<String, Object> contrato = new LinkedHashMap<>();
        contrato.put("tipo", tipo);
        contrato.put("fechaInicio", inicio);
        contrato.put("sueldoBasico", sueldo);
        contrato.put("jornadaSemanal", 48);
        contrato.put("regimenPensionario", regimen);
        return contrato;
    }
}
