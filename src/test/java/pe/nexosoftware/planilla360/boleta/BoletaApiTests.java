package pe.nexosoftware.planilla360.boleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import pe.nexosoftware.planilla360.boleta.pdf.GeneradorBoletaPdf;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.PlanillaCerradaBase;
import tools.jackson.databind.JsonNode;

/**
 * Boletas por la API sin mensajería (el cierre genera las boletas en el mismo proceso y el PDF queda en el almacén en
 * memoria): generación, estado, descarga por rol, aislamiento entre empresas e idempotencia.
 */
@DisplayName("API de boletas")
class BoletaApiTests extends PlanillaCerradaBase {

    @MockitoSpyBean
    private GeneradorBoletaPdf generador;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private UUID planilla;

    @BeforeEach
    void escenario() throws Exception {
        prepararEmpresaConTrabajadores();
        planilla = cerrarPlanillaDeMarzo();
    }

    private JsonNode estado(Usuario como) throws Exception {
        return json.readTree(leer(como, PLANILLAS + "/" + planilla + "/boletas/estado"));
    }

    private JsonNode boletas(Usuario como) throws Exception {
        return json.readTree(leer(como, PLANILLAS + "/" + planilla + "/boletas")).get("contenido");
    }

    @Test
    @DisplayName("al cerrar la planilla se genera una boleta por trabajador")
    void cierreGeneraBoletas() throws Exception {
        JsonNode estado = estado(e.rrhh());

        assertThat(estado.get("estado").asString()).isEqualTo("COMPLETADA");
        assertThat(estado.get("total").asInt()).isEqualTo(3);
        assertThat(estado.get("generadas").asInt()).isEqualTo(3);
        assertThat(estado.get("fallidas").asInt()).isZero();
    }

    @Test
    @DisplayName("RRHH descarga el PDF de una boleta")
    void rrhhDescargaPdf() throws Exception {
        String id = boletas(e.rrhh()).get(0).get("id").asString();

        byte[] pdf = mvc.perform(get("/api/v1/boletas/" + id + "/pdf").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("el trabajador ve y descarga solo sus boletas; las ajenas responden 404")
    void trabajadorSoloVeLasSuyas() throws Exception {
        Usuario deAna = usuarioDeEmpleado(e, ana);
        JsonNode todas = boletas(e.rrhh());
        String suya = null;
        String ajena = null;
        for (JsonNode b : todas) {
            if (ana.toString().equals(b.get("empleadoId").asString())) {
                suya = b.get("id").asString();
            } else {
                ajena = b.get("id").asString();
            }
        }

        JsonNode mias = json.readTree(leer(deAna, "/api/v1/boletas/mias"));
        assertThat(mias.get("totalElementos").asInt()).isEqualTo(1);
        assertThat(mias.get("contenido").get(0).get("id").asString()).isEqualTo(suya);
        mvc.perform(get("/api/v1/boletas/" + suya + "/pdf").header("Authorization", bearer(deAna)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas/" + ajena + "/pdf").header("Authorization", bearer(deAna)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("otra empresa no ve las boletas (404) y el trabajador no puede generar")
    void aislamientoYPermisos() throws Exception {
        String id = boletas(e.rrhh()).get(0).get("id").asString();
        Escenario otra = nuevoEscenario();

        mvc.perform(get("/api/v1/boletas/" + id + "/pdf").header("Authorization", bearer(otra.rrhh())))
                .andExpect(status().isNotFound());
        mvc.perform(post(PLANILLAS + "/" + planilla + "/boletas/generar")
                        .header("Authorization", bearer(usuarioDeEmpleado(e, ana))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("repetir la generación no duplica boletas")
    void generarEsIdempotente() throws Exception {
        mvc.perform(post(PLANILLAS + "/" + planilla + "/boletas/generar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isAccepted());
        mvc.perform(post(PLANILLAS + "/" + planilla + "/boletas/generar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isAccepted());

        Integer filas = jdbc.queryForObject("select count(*) from boleta where planilla_id = ?", Integer.class, planilla);
        assertThat(filas).isEqualTo(3);
    }

    @Test
    @DisplayName("no se generan boletas de una planilla que no está cerrada")
    void soloPlanillasCerradas() throws Exception {
        String creada = enviar(e.rrhh(), PLANILLAS, java.util.Map.of("periodo", "2030-02", "tipo", "MENSUAL"))
                .andReturn().getResponse().getContentAsString();
        String borrador = json.readTree(creada).get("id").asString();

        mvc.perform(post(PLANILLAS + "/" + borrador + "/boletas/generar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("si falla el PDF la boleta queda FALLIDA y se recupera al reintentar")
    void fallaYRecuperacion() throws Exception {
        // Se anula el trabajo hecho en el cierre para repetir la generación con un fallo controlado.
        jdbc.update("delete from boleta where planilla_id = ?", planilla);
        doThrow(new IllegalStateException("PDF roto")).when(generador).generar(any());

        mvc.perform(post(PLANILLAS + "/" + planilla + "/boletas/generar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isAccepted());
        JsonNode conFallos = estado(e.rrhh());
        assertThat(conFallos.get("estado").asString()).isEqualTo("CON_FALLOS");
        assertThat(conFallos.get("fallidas").asInt()).isEqualTo(3);

        doReturn(new byte[] {'%', 'P', 'D', 'F', '-'}).when(generador).generar(any());
        mvc.perform(post(PLANILLAS + "/" + planilla + "/boletas/generar").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isAccepted());

        assertThat(estado(e.rrhh()).get("estado").asString()).isEqualTo("COMPLETADA");
    }
}
