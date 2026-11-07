package pe.nexosoftware.planilla360.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;

class CatalogoParametrosTests extends IntegracionBase {

    @Autowired
    private CatalogoApi catalogo;

    @Test
    void losParametrosLegalesSembradosEstanDisponiblesPorApiPublica() {
        LocalDate hoy = LocalDate.of(2026, 11, 7);

        assertThat(catalogo.valorParametro("RMV", hoy)).isEqualByComparingTo("1130.00");
        assertThat(catalogo.valorParametro("UIT", hoy)).isEqualByComparingTo("5350.00");
        assertThat(catalogo.valorParametro("ONP_PCT", hoy)).isEqualByComparingTo("13");
        assertThat(catalogo.afps()).extracting(AfpResumen::nombre)
                .containsExactly("Habitat", "Integra", "Prima", "Profuturo");
    }

    @Test
    void registrarUnaNuevaVigenciaCierraLaAnteriorYRefrescaLaCache() throws Exception {
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);
        String codigo = "PRUEBA_" + System.nanoTime();
        LocalDate inicio = LocalDate.of(2030, 1, 1);

        registrar(saas, codigo, "10.0000", inicio);
        // Se consulta antes de registrar la segunda: queda en caché el valor viejo.
        assertThat(catalogo.valorParametro(codigo, inicio.plusYears(2))).isEqualByComparingTo(new BigDecimal("10"));

        registrar(saas, codigo, "12.5000", inicio.plusYears(1));

        assertThat(catalogo.valorParametro(codigo, inicio.plusMonths(6))).isEqualByComparingTo("10");
        assertThat(catalogo.valorParametro(codigo, inicio.plusYears(1))).isEqualByComparingTo("12.5");
        assertThat(catalogo.valorParametro(codigo, inicio.plusYears(2))).isEqualByComparingTo("12.5");
    }

    @Test
    void unaVigenciaNoPuedeEmpezarAntesDeLaVigenteActual() throws Exception {
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);
        String codigo = "PRUEBA_" + System.nanoTime();
        registrar(saas, codigo, "10.0000", LocalDate.of(2030, 6, 1));

        mvc.perform(post("/api/v1/parametros-legales").header("Authorization", bearer(saas))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "codigo", codigo, "descripcion", "Retroactivo", "valor", "9.0", "vigenteDesde", "2030-01-01"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void elCodigoDelParametroTieneFormatoValido() throws Exception {
        Usuario saas = nuevoUsuario(null, Rol.ADMIN_SAAS);

        mvc.perform(post("/api/v1/parametros-legales").header("Authorization", bearer(saas))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "codigo", "minusculas", "descripcion", "x", "valor", "1", "vigenteDesde", "2030-01-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("codigo"));
    }

    @Test
    void feriadosDelAnioSeListanOrdenados() throws Exception {
        Usuario empleado = nuevoUsuario(nuevaEmpresa(), Rol.EMPLEADO);

        mvc.perform(get("/api/v1/feriados?anio=2026").header("Authorization", bearer(empleado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Año Nuevo"))
                .andExpect(jsonPath("$[-1].descripcion").value("Navidad"));
        mvc.perform(get("/api/v1/feriados?anio=1850").header("Authorization", bearer(empleado)))
                .andExpect(status().isBadRequest());
    }

    private void registrar(Usuario como, String codigo, String valor, LocalDate desde) throws Exception {
        mvc.perform(post("/api/v1/parametros-legales").header("Authorization", bearer(como))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "codigo", codigo, "descripcion", "Parámetro de prueba", "valor", valor,
                                "vigenteDesde", desde.toString()))))
                .andExpect(status().isCreated());
    }
}
