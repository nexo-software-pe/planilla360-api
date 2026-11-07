package pe.nexosoftware.planilla360;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;

class Planilla360ApiApplicationTests extends IntegracionBase {

    @Test
    void healthEsPublicoYReportaUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void probesDeLivenessYReadinessSonPublicas() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void cualquierOtroEndpointRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/empleados"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void laDocumentacionOpenApiDeclaraLaSeguridadJwt() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/empresas']").exists())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"));
    }
}
