package pe.nexosoftware.planilla360.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.PlanillaCerradaBase;
import tools.jackson.databind.JsonNode;

/** Reportes con SQL nativo: ranking con RANK(), ROLLUP, función de ventana, Excel y aislamiento entre empresas. */
@DisplayName("API de reportes")
class ReporteApiTests extends PlanillaCerradaBase {

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void escenario() throws Exception {
        prepararEmpresaConTrabajadores();
        cerrarPlanillaDeMarzo();
    }

    private void tardanza(UUID empleado, String fecha, int minutos) {
        jdbc.update("insert into asistencia_diaria (tenant_id, empleado_id, fecha, minutos_tardanza, estado) "
                + "values (?, ?, ?::date, ?, 'TARDANZA')", e.empresa().getId(), empleado, fecha, minutos);
    }

    private JsonNode consultar(Usuario como, String ruta) throws Exception {
        return json.readTree(leer(como, ruta));
    }

    @Test
    @DisplayName("tardanzas por área: RANK comparte lugar en los empates y deja el salto siguiente")
    void tardanzasConEmpates() throws Exception {
        tardanza(ana, "2030-03-04", 30);
        tardanza(ana, "2030-03-05", 20);
        tardanza(beto, "2030-03-04", 50);
        tardanza(carla, "2030-03-04", 10);

        JsonNode filas = consultar(e.rrhh(), "/api/v1/reportes/tardanzas-por-area?periodo=2030-03&top=3");

        assertThat(filas).hasSize(3);
        assertThat(filas.get(0).get("minutos").asInt()).isEqualTo(50);
        assertThat(filas.get(0).get("posicion").asInt()).isEqualTo(1);
        assertThat(filas.get(1).get("posicion").asInt()).isEqualTo(1);
        assertThat(filas.get(2).get("trabajador").asString()).startsWith("Planilla, Carla");
        assertThat(filas.get(2).get("posicion").asInt()).isEqualTo(3);
        assertThat(filas.get(0).get("area").asString()).isEqualTo("Producción");
    }

    @Test
    @DisplayName("tardanzas por área: top limita los lugares y otro mes no se mezcla")
    void tardanzasConTop() throws Exception {
        tardanza(ana, "2030-03-04", 30);
        tardanza(beto, "2030-03-04", 50);
        tardanza(carla, "2030-03-04", 10);
        tardanza(carla, "2030-02-04", 99);

        JsonNode filas = consultar(e.rrhh(), "/api/v1/reportes/tardanzas-por-area?periodo=2030-03&top=1");

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).get("minutos").asInt()).isEqualTo(50);
    }

    @Test
    @DisplayName("tardanzas por área: parámetros inválidos responden 422")
    void tardanzasParametros() throws Exception {
        mvc.perform(get("/api/v1/reportes/tardanzas-por-area?periodo=marzo").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/reportes/tardanzas-por-area?periodo=2030-03&top=0")
                        .header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("costo laboral: subtotal por área y total de la empresa (ROLLUP) con ingresos + EsSalud")
    void costoLaboral() throws Exception {
        JsonNode filas = consultar(e.rrhh(), "/api/v1/reportes/costo-laboral?periodo=2030-03");

        assertThat(filas).hasSize(3);
        JsonNode cargo = filas.get(0);
        JsonNode area = filas.get(1);
        JsonNode empresa = filas.get(2);
        assertThat(cargo.get("nivel").asString()).isEqualTo("CARGO");
        assertThat(area.get("nivel").asString()).isEqualTo("AREA");
        assertThat(empresa.get("nivel").asString()).isEqualTo("EMPRESA");
        assertThat(empresa.get("trabajadores").asInt()).isEqualTo(3);
        BigDecimal ingresos = new BigDecimal(empresa.get("ingresos").asString());
        BigDecimal essalud = new BigDecimal(empresa.get("essalud").asString());
        assertThat(new BigDecimal(empresa.get("costoTotal").asString())).isEqualByComparingTo(ingresos.add(essalud));
        assertThat(new BigDecimal(area.get("costoTotal").asString()))
                .isEqualByComparingTo(new BigDecimal(empresa.get("costoTotal").asString()));
    }

    @Test
    @DisplayName("vacaciones pendientes: saldo por periodo y saldo total del trabajador (función de ventana)")
    void vacacionesPendientes() throws Exception {
        jdbc.update("insert into periodo_vacacional (tenant_id, empleado_id, desde, hasta, dias_ganados, dias_gozados) "
                + "values (?, ?, date '2028-01-01', date '2028-12-31', 30, 10)", e.empresa().getId(), ana);
        jdbc.update("insert into periodo_vacacional (tenant_id, empleado_id, desde, hasta, dias_ganados, dias_gozados) "
                + "values (?, ?, date '2029-01-01', date '2029-12-31', 30, 30)", e.empresa().getId(), ana);
        jdbc.update("insert into periodo_vacacional (tenant_id, empleado_id, desde, hasta, dias_ganados, dias_gozados) "
                + "values (?, ?, date '2030-01-01', date '2030-12-31', 30, 0)", e.empresa().getId(), ana);

        JsonNode filas = consultar(e.rrhh(), "/api/v1/reportes/vacaciones-pendientes");

        assertThat(filas).hasSize(2); // el periodo sin saldo no aparece
        assertThat(filas.get(0).get("saldoTotalDias").asInt()).isEqualTo(50);
        assertThat(filas.get(0).get("saldoDias").asInt()).isEqualTo(20);
        assertThat(filas.get(0).get("vencido").asBoolean()).isTrue();
        assertThat(filas.get(1).get("saldoDias").asInt()).isEqualTo(30);
        assertThat(filas.get(1).get("vencido").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("el Excel de la planilla trae un renglón por trabajador")
    void excelDeLaPlanilla() throws Exception {
        byte[] libro = mvc.perform(get("/api/v1/reportes/planilla-mensual.xlsx?periodo=2030-03")
                        .header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook excel = new XSSFWorkbook(new ByteArrayInputStream(libro))) {
            Sheet hoja = excel.getSheetAt(0);
            assertThat(hoja.getLastRowNum()).isEqualTo(3);
            assertThat(hoja.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Trabajador");
            assertThat(hoja.getRow(1).getCell(11).getNumericCellValue()).isPositive();
        }
    }

    @Test
    @DisplayName("otra empresa no ve nada de estos datos y el trabajador no accede a los reportes")
    void aislamientoYPermisos() throws Exception {
        tardanza(ana, "2030-03-04", 30);
        Escenario otra = nuevoEscenario();

        assertThat(consultar(otra.rrhh(), "/api/v1/reportes/tardanzas-por-area?periodo=2030-03")).isEmpty();
        assertThat(consultar(otra.rrhh(), "/api/v1/reportes/costo-laboral?periodo=2030-03")).isEmpty();
        assertThat(consultar(otra.rrhh(), "/api/v1/reportes/vacaciones-pendientes")).isEmpty();
        mvc.perform(get("/api/v1/reportes/costo-laboral?periodo=2030-03")
                        .header("Authorization", bearer(usuarioDeEmpleado(e, ana))))
                .andExpect(status().isForbidden());
    }
}
