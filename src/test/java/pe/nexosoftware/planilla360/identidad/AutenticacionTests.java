package pe.nexosoftware.planilla360.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;
import tools.jackson.databind.JsonNode;

class AutenticacionTests extends IntegracionBase {

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
    }

    private JsonNode cuerpo(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    @Test
    void loginCorrectoEntregaTokensYElPerfilSinDatosSensibles() throws Exception {
        Usuario rrhh = nuevoUsuario(nuevaEmpresa(), Rol.RRHH);

        MvcResult resultado = login(rrhh.getEmail(), CLAVE_PRUEBA);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = cuerpo(resultado);
        assertThat(body.get("accessToken").asString()).isNotBlank();
        assertThat(body.get("refreshToken").asString()).isNotBlank();
        assertThat(body.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(body.get("usuario").get("rol").asString()).isEqualTo("RRHH");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("passwordHash").doesNotContain("$2a$");
    }

    @Test
    void elAccessTokenPermiteConsultarElPerfilConSuEmpresa() throws Exception {
        Empresa empresa = nuevaEmpresa();
        Usuario rrhh = nuevoUsuario(empresa, Rol.RRHH);
        String access = cuerpo(login(rrhh.getEmail(), CLAVE_PRUEBA)).get("accessToken").asString();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value(rrhh.getEmail()))
                .andExpect(jsonPath("$.empresa.ruc").value(empresa.getRuc()))
                .andExpect(jsonPath("$.empresa.razonSocial").value(empresa.getRazonSocial()));
    }

    @Test
    void adminDeLaPlataformaNoTieneEmpresa() throws Exception {
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);
        String access = cuerpo(login(saas.getEmail(), CLAVE_PRUEBA)).get("accessToken").asString();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN_SAAS"))
                .andExpect(jsonPath("$.empresa").doesNotExist());
    }

    @Test
    void passwordIncorrectaResponde401ConProblemDetail() throws Exception {
        Usuario rrhh = nuevoUsuario(nuevaEmpresa(), Rol.RRHH);

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", rrhh.getEmail(), "password", "equivocada-123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas."));
    }

    @Test
    void correoInexistenteNoSeDistingueDeClaveIncorrecta() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@ejemplo.pe\",\"password\":\"cualquiera-123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas."));
    }

    @Test
    void datosMalFormadosDevuelven400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-correo\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void despuesDelLimiteDeIntentosResponde429ConRetryAfter() throws Exception {
        String email = "limite." + java.util.UUID.randomUUID() + "@ejemplo.pe";
        for (int i = 0; i < 5; i++) {
            assertThat(login(email, "equivocada-123").getResponse().getStatus()).isEqualTo(401);
        }

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "equivocada-123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void refrescarRotaElTokenYElAnteriorYaNoSirve() throws Exception {
        Usuario rrhh = nuevoUsuario(nuevaEmpresa(), Rol.RRHH);
        String refresh1 = cuerpo(login(rrhh.getEmail(), CLAVE_PRUEBA)).get("refreshToken").asString();

        MvcResult segundo = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh1))))
                .andExpect(status().isOk())
                .andReturn();
        String refresh2 = cuerpo(segundo).get("refreshToken").asString();
        assertThat(refresh2).isNotEqualTo(refresh1);

        // Reutilizar el primero indica robo: se rechaza y se invalida también el segundo.
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh1))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refresh2))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cerrarSesionRevocaElRefreshToken() throws Exception {
        Usuario rrhh = nuevoUsuario(nuevaEmpresa(), Rol.RRHH);
        String refresh = cuerpo(login(rrhh.getEmail(), CLAVE_PRUEBA)).get("refreshToken").asString();
        String cuerpoRefresh = json.writeValueAsString(Map.of("refreshToken", refresh));

        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(cuerpoRefresh))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(cuerpoRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenInventadoSeRechaza() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"inventado\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void empresaInactivaNoPuedeIniciarSesion() throws Exception {
        Empresa empresa = nuevaEmpresa();
        Usuario rrhh = nuevoUsuario(empresa, Rol.RRHH);
        empresa.cambiarEstado(false);
        empresas.save(empresa);

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", rrhh.getEmail(), "password", CLAVE_PRUEBA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarPasswordRevocaLasSesionesYExigeLaClaveActual() throws Exception {
        Usuario rrhh = nuevoUsuario(nuevaEmpresa(), Rol.RRHH);
        JsonNode sesion = cuerpo(login(rrhh.getEmail(), CLAVE_PRUEBA));
        String access = sesion.get("accessToken").asString();

        mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", "no-es-la-actual1", "passwordNueva", "Nueva-Clave-456"))))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", CLAVE_PRUEBA, "passwordNueva", "Nueva-Clave-456"))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", sesion.get("refreshToken").asString()))))
                .andExpect(status().isUnauthorized());
        assertThat(login(rrhh.getEmail(), "Nueva-Clave-456").getResponse().getStatus()).isEqualTo(200);
    }
}
