package pe.nexosoftware.planilla360.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;

/** Endpoints de consulta externa en modo stub (sin servicio externo): rutas, validación y permisos por rol. */
@DisplayName("API de consultas externas")
class ConsultaExternaApiTests extends IntegracionBase {

    private Empresa empresa;
    private Usuario rrhh;

    @BeforeEach
    void escenario() {
        empresa = nuevaEmpresa();
        rrhh = nuevoUsuario(empresa, Rol.RRHH);
    }

    @Test
    @DisplayName("RRHH consulta DNI, RUC y tipo de cambio")
    void consultas() throws Exception {
        String dni = mvc.perform(get("/api/v1/consultas/dni/45678901").header("Authorization", bearer(rrhh)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String ruc = mvc.perform(get("/api/v1/consultas/ruc/20601234565").header("Authorization", bearer(rrhh)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String tc = mvc.perform(get("/api/v1/consultas/tipo-cambio").header("Authorization", bearer(rrhh)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(dni).get("apellidoPaterno").asString()).isEqualTo("PEREZ");
        assertThat(json.readTree(ruc).get("estado").asString()).isEqualTo("ACTIVO");
        assertThat(json.readTree(tc).get("moneda").asString()).isEqualTo("USD");
        assertThat(json.readTree(tc).get("desactualizado").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("un DNI inexistente responde 404 y uno mal formado 422")
    void errores() throws Exception {
        mvc.perform(get("/api/v1/consultas/dni/00000000").header("Authorization", bearer(rrhh)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/consultas/dni/123").header("Authorization", bearer(rrhh)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/consultas/ruc/20601234560").header("Authorization", bearer(rrhh)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("el trabajador no consulta y sin sesión responde 401")
    void permisos() throws Exception {
        Usuario trabajador = nuevoUsuario(empresa, Rol.EMPLEADO);

        mvc.perform(get("/api/v1/consultas/tipo-cambio").header("Authorization", bearer(trabajador)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/consultas/tipo-cambio")).andExpect(status().isUnauthorized());
    }
}
