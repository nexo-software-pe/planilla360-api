package pe.nexosoftware.planilla360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;

/** Matriz de acceso: cada rol solo entra a lo que le corresponde. */
class AccesoPorRolTests extends IntegracionBase {

    @Autowired
    private JwtEncoder encoder;

    private static final String AREA = "{\"nombre\":\"Área de prueba\"}";
    private static final String CARGO =
            "{\"areaId\":\"00000000-0000-4000-8000-000000000000\",\"nombre\":\"Cargo\",\"sueldoReferencial\":1500}";
    private static final String PARAMETRO =
            "{\"codigo\":\"CODIGO_X\",\"descripcion\":\"x\",\"valor\":1,\"vigenteDesde\":\"2031-01-01\"}";
    private static final String VACIO = "";

    static Stream<Arguments> matriz() {
        return Stream.of(
                // rol, método, ruta, cuerpo, estado esperado (cuerpos válidos: lo que decide es la autorización)
                Arguments.of(Rol.ADMIN_SAAS, "GET", "/api/v1/empresas", VACIO, 200),
                Arguments.of(Rol.ADMIN_EMPRESA, "GET", "/api/v1/empresas", VACIO, 403),
                Arguments.of(Rol.RRHH, "GET", "/api/v1/empresas", VACIO, 403),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/empresas", VACIO, 403),
                Arguments.of(Rol.ADMIN_SAAS, "GET", "/api/v1/usuarios", VACIO, 200),
                Arguments.of(Rol.ADMIN_EMPRESA, "GET", "/api/v1/usuarios", VACIO, 200),
                Arguments.of(Rol.RRHH, "GET", "/api/v1/usuarios", VACIO, 403),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/usuarios", VACIO, 403),
                Arguments.of(Rol.ADMIN_EMPRESA, "GET", "/api/v1/auditoria", VACIO, 200),
                Arguments.of(Rol.RRHH, "GET", "/api/v1/auditoria", VACIO, 403),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/auditoria", VACIO, 403),
                Arguments.of(Rol.ADMIN_EMPRESA, "POST", "/api/v1/areas", AREA, 201),
                Arguments.of(Rol.RRHH, "POST", "/api/v1/areas", AREA, 201),
                Arguments.of(Rol.EMPLEADO, "POST", "/api/v1/areas", AREA, 403),
                Arguments.of(Rol.ADMIN_SAAS, "POST", "/api/v1/areas", AREA, 403),
                Arguments.of(Rol.EMPLEADO, "POST", "/api/v1/cargos", CARGO, 403),
                Arguments.of(Rol.RRHH, "POST", "/api/v1/cargos", CARGO, 404),
                Arguments.of(Rol.RRHH, "POST", "/api/v1/parametros-legales", PARAMETRO, 403),
                Arguments.of(Rol.ADMIN_EMPRESA, "POST", "/api/v1/parametros-legales", PARAMETRO, 403),
                // lectura de catálogos: cualquier usuario autenticado
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/areas", VACIO, 200),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/cargos", VACIO, 200),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/afps", VACIO, 200),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/parametros-legales", VACIO, 200),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/feriados", VACIO, 200),
                Arguments.of(Rol.EMPLEADO, "GET", "/api/v1/auth/me", VACIO, 200));
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {4}")
    @MethodSource("matriz")
    void cadaRolSoloAccedeAlRecursoQueLeCorresponde(Rol rol, String metodo, String ruta, String cuerpo, int esperado)
            throws Exception {
        Empresa empresa = nuevaEmpresa();
        Usuario usuario = nuevoUsuario(empresa, rol);
        MockHttpServletRequestBuilder peticion = metodo.equals("GET") ? get(ruta) : post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo);

        mvc.perform(peticion.header("Authorization", bearer(usuario)))
                .andExpect(status().is(esperado));
    }

    @Test
    void tokenMalFormadoEsRechazado() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer esto.no.es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void tokenExpiradoEsRechazado() throws Exception {
        Usuario usuario = nuevoUsuario(nuevaEmpresa(), Rol.RRHH);
        Instant hace2Horas = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("planilla360").subject(usuario.getId().toString())
                .issuedAt(hace2Horas).expiresAt(hace2Horas.plus(Duration.ofMinutes(15)))
                .claim("rol", "RRHH").claim("email", usuario.getEmail())
                .claim("tenant", usuario.getTenantId().toString()).build();
        String vencido = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOtroEmisorEsRechazado() throws Exception {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("otro-emisor").subject(UUID.randomUUID().toString())
                .issuedAt(ahora).expiresAt(ahora.plus(Duration.ofMinutes(15)))
                .claim("rol", "ADMIN_SAAS").claim("email", "intruso@ejemplo.pe").build();
        String ajeno = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/v1/empresas").header("Authorization", "Bearer " + ajeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmadoConOtraClaveEsRechazado() throws Exception {
        // Payload válido pero firmado con un secreto que no es el del servidor.
        com.nimbusds.jose.jwk.source.ImmutableSecret<com.nimbusds.jose.proc.SecurityContext> otraClave =
                new com.nimbusds.jose.jwk.source.ImmutableSecret<>(new byte[48]);
        JwtEncoder impostor = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(otraClave);
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("planilla360").subject(UUID.randomUUID().toString())
                .issuedAt(ahora).expiresAt(ahora.plus(Duration.ofMinutes(15)))
                .claim("rol", "ADMIN_SAAS").claim("email", "intruso@ejemplo.pe").build();
        String falso = impostor.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/v1/empresas").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized());
        assertThat(falso).isNotBlank();
    }
}
