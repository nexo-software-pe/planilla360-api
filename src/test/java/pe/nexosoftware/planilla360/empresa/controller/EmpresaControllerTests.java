package pe.nexosoftware.planilla360.empresa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.infrastructure.WebSliceConfig;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;
import pe.nexosoftware.planilla360.empresa.dto.EmpresaResponse;
import pe.nexosoftware.planilla360.empresa.service.EmpresaService;

@WebMvcTest(EmpresaController.class)
@Import(WebSliceConfig.class)
class EmpresaControllerTests {

    private static final String CUERPO_VALIDO = """
            {"ruc":"20601234565","razonSocial":"Textiles Andinos S.A.C.","regimenLaboral":"GENERAL","plan":"PROFESIONAL"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmpresaService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor como(String rol) {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("rol", rol).claim("email", "x@ejemplo.pe"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void sinTokenResponde401EnFormatoProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/empresas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void rrhhNoPuedeListarEmpresas() throws Exception {
        mvc.perform(get("/api/v1/empresas").with(como("RRHH")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        verifyNoInteractions(service);
    }

    @Test
    void adminSaasListaEmpresasPaginadas() throws Exception {
        when(service.listar(any(), any())).thenReturn(new Pagina<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/empresas").with(como("ADMIN_SAAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void crearEmpresaValidaResponde201ConLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.crear(any())).thenReturn(new EmpresaResponse(
                id, "20601234565", "Textiles Andinos S.A.C.", RegimenLaboral.GENERAL, PlanSaas.PROFESIONAL, true,
                Instant.parse("2026-11-07T12:00:00Z")));

        mvc.perform(post("/api/v1/empresas").with(como("ADMIN_SAAS")).contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/empresas/" + id))
                .andExpect(jsonPath("$.ruc").value("20601234565"));
    }

    @Test
    void rucConDigitoVerificadorIncorrectoDevuelve400ConDetalleDelCampo() throws Exception {
        mvc.perform(post("/api/v1/empresas").with(como("ADMIN_SAAS")).contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO.replace("20601234565", "20601234566")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errores[0].campo").value("ruc"));
        verifyNoInteractions(service);
    }

    @Test
    void camposObligatoriosFaltantesSeReportanTodos() throws Exception {
        mvc.perform(post("/api/v1/empresas").with(como("ADMIN_SAAS")).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(4));
    }

    @Test
    void rucDuplicadoSeTraduceA409() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictoException("Ya existe una empresa con el RUC 20601234565."));

        mvc.perform(post("/api/v1/empresas").with(como("ADMIN_SAAS")).contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Ya existe una empresa con el RUC 20601234565."));
    }

    @Test
    void ordenarPorUnCampoInexistenteDevuelve400() throws Exception {
        when(service.listar(any(), any())).thenThrow(
                new org.springframework.data.core.PropertyReferenceException(
                        "noExiste", org.springframework.data.core.TypeInformation.of(EmpresaResponse.class), List.of()));

        mvc.perform(get("/api/v1/empresas?sort=noExiste").with(como("ADMIN_SAAS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro inválido"));
    }
}
