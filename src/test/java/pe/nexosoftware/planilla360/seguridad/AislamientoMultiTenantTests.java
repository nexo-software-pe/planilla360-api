package pe.nexosoftware.planilla360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;
import tools.jackson.databind.JsonNode;

/** Una empresa nunca ve ni modifica datos de otra, aunque conozca los identificadores. */
class AislamientoMultiTenantTests extends IntegracionBase {

    private Empresa empresaA;
    private Empresa empresaB;
    private Usuario rrhhA;
    private Usuario rrhhB;
    private Usuario adminA;
    private Usuario adminB;

    @BeforeEach
    void datos() {
        empresaA = nuevaEmpresa();
        empresaB = nuevaEmpresa();
        rrhhA = nuevoUsuario(empresaA, Rol.RRHH);
        rrhhB = nuevoUsuario(empresaB, Rol.RRHH);
        adminA = nuevoUsuario(empresaA, Rol.ADMIN_EMPRESA);
        adminB = nuevoUsuario(empresaB, Rol.ADMIN_EMPRESA);
    }

    private UUID crearArea(Usuario como, String nombre) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/areas").header("Authorization", bearer(como))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(respuesta).get("id").asString());
    }

    @Test
    void cadaEmpresaSoloListaSusAreas() throws Exception {
        crearArea(rrhhA, "Producción A");
        crearArea(rrhhB, "Logística B");

        mvc.perform(get("/api/v1/areas").header("Authorization", bearer(rrhhA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Producción A"));
        mvc.perform(get("/api/v1/areas").header("Authorization", bearer(rrhhB)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Logística B"));
    }

    @Test
    void unAreaAjenaSeTrataComoInexistenteEnLecturaEdicionYBorrado() throws Exception {
        UUID areaDeA = crearArea(rrhhA, "Producción A");
        String cuerpo = json.writeValueAsString(Map.of("nombre", "Secuestrada"));

        mvc.perform(get("/api/v1/areas/" + areaDeA).header("Authorization", bearer(rrhhB)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/areas/" + areaDeA).header("Authorization", bearer(rrhhB))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/areas/" + areaDeA).header("Authorization", bearer(rrhhB)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/areas/" + areaDeA).header("Authorization", bearer(rrhhA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Producción A"));
    }

    @Test
    void noSePuedeCrearUnCargoEnUnAreaDeOtraEmpresa() throws Exception {
        UUID areaDeA = crearArea(rrhhA, "Producción A");

        mvc.perform(post("/api/v1/cargos").header("Authorization", bearer(rrhhB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "areaId", areaDeA.toString(), "nombre", "Intruso", "sueldoReferencial", "1500.00"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void elMismoNombreDeAreaPuedeExistirEnEmpresasDistintas() throws Exception {
        crearArea(rrhhA, "Administración");
        crearArea(rrhhB, "Administración");
        mvc.perform(post("/api/v1/areas").header("Authorization", bearer(rrhhA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombre", "administración"))))
                .andExpect(status().isConflict());
    }

    @Test
    void elAdminDeEmpresaSoloVeLosUsuariosDeSuEmpresa() throws Exception {
        String respuesta = mvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(adminA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode contenido = json.readTree(respuesta).get("contenido");
        assertThat(contenido).hasSize(2);
        contenido.forEach(u -> assertThat(u.get("tenantId").asString()).isEqualTo(empresaA.getId().toString()));
    }

    @Test
    void elAdminDeEmpresaNoPuedeTocarUsuariosDeOtraEmpresa() throws Exception {
        mvc.perform(get("/api/v1/usuarios/" + rrhhB.getId()).header("Authorization", bearer(adminA)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/usuarios/" + rrhhB.getId() + "/estado").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/usuarios/" + rrhhB.getId() + "/reset-password").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"passwordNueva\":\"Hackeada-12345\"}"))
                .andExpect(status().isNotFound());

        assertThat(usuarios.findById(rrhhB.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void elAdminDeEmpresaNoPuedeCrearUsuariosEnOtraEmpresa() throws Exception {
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "infiltrado." + UUID.randomUUID() + "@ejemplo.pe", "nombre", "Infiltrado",
                                "password", "Clave-Segura-123", "rol", "RRHH", "tenantId", empresaB.getId().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdminDeEmpresaCreaUsuariosSiempreEnSuPropiaEmpresa() throws Exception {
        String correo = "nuevo." + UUID.randomUUID() + "@ejemplo.pe";
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", correo, "nombre", "Nuevo Usuario", "password", "Clave-Segura-123", "rol", "EMPLEADO"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(empresaA.getId().toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void elAdminDeEmpresaNoPuedeCrearAdministradoresDeLaPlataforma() throws Exception {
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "saas." + UUID.randomUUID() + "@ejemplo.pe", "nombre", "Falso Admin",
                                "password", "Clave-Segura-123", "rol", "ADMIN_SAAS"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void laAuditoriaSoloMuestraLosCambiosDeLaPropiaEmpresa() throws Exception {
        UUID areaA = crearArea(rrhhA, "Área auditada A");
        crearArea(rrhhB, "Área auditada B");

        String respuesta = mvc.perform(get("/api/v1/auditoria?entidad=AREA").header("Authorization", bearer(adminA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode contenido = json.readTree(respuesta).get("contenido");
        assertThat(contenido).hasSize(1);
        JsonNode registro = contenido.get(0);
        assertThat(registro.get("entidadId").asString()).isEqualTo(areaA.toString());
        assertThat(registro.get("accion").asString()).isEqualTo("CREAR");
        assertThat(registro.get("usuario").asString()).isEqualTo(rrhhA.getEmail());
        assertThat(registro.get("despues").get("nombre").asString()).isEqualTo("Área auditada A");
        assertThat(registro.get("antes").isNull()).isTrue();

        mvc.perform(get("/api/v1/auditoria?entidad=AREA").header("Authorization", bearer(adminB)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].despues.nombre").value("Área auditada B"));
    }

    @Test
    void elAdminDeEmpresaNoPuedeVerLaAuditoriaDeOtraAunqueLaPida() throws Exception {
        crearArea(rrhhB, "Solo de B");

        mvc.perform(get("/api/v1/auditoria?entidad=AREA&tenantId=" + empresaB.getId())
                        .header("Authorization", bearer(adminA)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void elAdminDeLaPlataformaPuedeFiltrarUsuariosPorEmpresa() throws Exception {
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);

        mvc.perform(get("/api/v1/usuarios?tenantId=" + empresaB.getId()).header("Authorization", bearer(saas)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void elAdminDeLaPlataformaNoVeAreasPorqueNoPerteneceAEmpresaAlguna() throws Exception {
        crearArea(rrhhA, "Producción A");
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);

        mvc.perform(get("/api/v1/areas").header("Authorization", bearer(saas)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void desactivarUnUsuarioInvalidaSusSesionesYNoPuedeDesactivarseASiMismo() throws Exception {
        mvc.perform(patch("/api/v1/usuarios/" + adminA.getId() + "/estado").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(patch("/api/v1/usuarios/" + rrhhA.getId() + "/estado").header("Authorization", bearer(adminA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
