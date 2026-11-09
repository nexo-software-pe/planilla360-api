package pe.nexosoftware.planilla360.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.soporte.PersonalBase;
import tools.jackson.databind.JsonNode;

/** Importación de empleados desde Excel: simulación, errores fila por fila, alta real y permisos. */
@DisplayName("API de importación de empleados")
class ImportacionApiTests extends PersonalBase {

    private static final String[] COLUMNAS = {
        "dni", "nombres", "apellidos", "fecha_nacimiento", "email", "telefono", "fecha_ingreso", "area", "cargo",
        "tipo_contrato", "fecha_fin_contrato", "sueldo_basico", "jornada_semanal", "regimen_pensionario", "afp",
        "tipo_comision"
    };

    private Escenario e;

    @BeforeEach
    void escenario() throws Exception {
        e = nuevoEscenario();
    }

    private static String[] fila(String dni, String regimen, String afp, String comision) {
        return new String[] {dni, "Carla", "Mendoza Ríos", "1994-06-12", "carla." + dni + "@ejemplo.pe", "987654321",
            "2024-03-01", "Producción", "Operario", "INDEFINIDO", "", "1500", "48", regimen, afp, comision};
    }

    private static MockMultipartFile excel(String nombre, String[]... filas) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Empleados");
            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < COLUMNAS.length; i++) {
                encabezado.createCell(i).setCellValue(COLUMNAS[i]);
            }
            int n = 1;
            for (String[] datos : filas) {
                Row fila = hoja.createRow(n++);
                for (int i = 0; i < datos.length; i++) {
                    if (!datos[i].isEmpty()) {
                        fila.createCell(i).setCellValue(datos[i]);
                    }
                }
            }
            libro.write(salida);
            return new MockMultipartFile("archivo", nombre,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", salida.toByteArray());
        }
    }

    private JsonNode importar(Usuario como, MockMultipartFile archivo, boolean simular) throws Exception {
        String respuesta = mvc.perform(multipart("/api/v1/importaciones/empleados").file(archivo)
                        .param("simular", String.valueOf(simular)).header("Authorization", bearer(como)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private String afp() throws Exception {
        return json.readTree(leer(e.rrhh(), "/api/v1/afps")).get(0).get("nombre").asString();
    }

    @Test
    @DisplayName("simular valida sin guardar nada")
    void simulacion() throws Exception {
        JsonNode resultado = importar(e.rrhh(), excel("empleados.xlsx", fila("45000001", "ONP", "", "")), true);

        assertThat(resultado.get("simulacion").asBoolean()).isTrue();
        assertThat(resultado.get("validas").asInt()).isEqualTo(1);
        assertThat(resultado.get("importadas").asInt()).isZero();
        assertThat(json.readTree(leer(e.rrhh(), "/api/v1/empleados?dni=45000001")).get("totalElementos").asInt())
                .isZero();
    }

    @Test
    @DisplayName("importar guarda al empleado con su contrato (ONP y AFP)")
    void importaEmpleados() throws Exception {
        JsonNode resultado = importar(e.rrhh(), excel("empleados.xlsx",
                fila("45000002", "ONP", "", ""), fila("45000003", "AFP", afp(), "FLUJO")), false);

        assertThat(resultado.get("importadas").asInt()).isEqualTo(2);
        assertThat(resultado.get("errores")).isEmpty();
        JsonNode lista = json.readTree(leer(e.rrhh(), "/api/v1/empleados?size=50"));
        assertThat(lista.get("totalElementos").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("reporta todos los errores con su fila y columna, e importa solo las filas válidas")
    void erroresPorFila() throws Exception {
        String[] sinDni = fila("", "ONP", "", "");
        String[] areaMala = fila("45000004", "ONP", "", "");
        areaMala[7] = "Inexistente";
        String[] afpSinComision = fila("45000005", "AFP", afp(), "");
        String[] onpConAfp = fila("45000006", "ONP", "Habitat", "");
        String[] duplicado = fila("45000007", "ONP", "", "");
        String[] valida = fila("45000008", "ONP", "", "");
        duplicado[3] = "no-es-fecha";

        JsonNode resultado = importar(e.rrhh(),
                excel("empleados.xlsx", sinDni, areaMala, afpSinComision, onpConAfp, duplicado, valida), false);

        assertThat(resultado.get("totalFilas").asInt()).isEqualTo(6);
        assertThat(resultado.get("importadas").asInt()).isEqualTo(1);
        List<String> errores = new java.util.ArrayList<>();
        resultado.get("errores").forEach(x -> errores.add(x.get("fila").asInt() + ":" + x.get("campo").asString()));
        assertThat(errores).contains("2:dni", "3:area", "4:tipo_comision", "5:afp", "6:fecha_nacimiento");
    }

    @Test
    @DisplayName("un DNI que ya existe se marca como error y no se duplica")
    void dniExistente() throws Exception {
        importar(e.rrhh(), excel("a.xlsx", fila("45000010", "ONP", "", "")), false);

        JsonNode resultado = importar(e.rrhh(), excel("b.xlsx", fila("45000010", "ONP", "", "")), false);

        assertThat(resultado.get("importadas").asInt()).isZero();
        assertThat(resultado.get("errores").get(0).get("mensaje").asString()).contains("Ya existe");
    }

    @Test
    @DisplayName("rechaza archivos que no son .xlsx o están dañados")
    void archivosInvalidos() throws Exception {
        MockMultipartFile csv = new MockMultipartFile("archivo", "datos.csv", "text/csv", "dni\n1".getBytes());
        MockMultipartFile roto = new MockMultipartFile("archivo", "roto.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "no es excel".getBytes());

        for (MockMultipartFile archivo : List.of(csv, roto)) {
            mvc.perform(multipart("/api/v1/importaciones/empleados").file(archivo)
                            .header("Authorization", bearer(e.rrhh())))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    @DisplayName("solo RRHH y el administrador de la empresa importan; hay una plantilla descargable")
    void permisosYPlantilla() throws Exception {
        Usuario trabajador = usuarioDeEmpleado(e, crearEmpleado(e, "Ana", "Prueba", "2024-01-01"));

        mvc.perform(multipart("/api/v1/importaciones/empleados").file(excel("x.xlsx"))
                        .header("Authorization", bearer(trabajador)))
                .andExpect(status().isForbidden());
        byte[] plantilla = mvc.perform(get("/api/v1/importaciones/empleados/plantilla.xlsx")
                        .header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook libro = new XSSFWorkbook(new java.io.ByteArrayInputStream(plantilla))) {
            assertThat(libro.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("dni");
        }
        assertThat(Map.of("columnas", COLUMNAS.length)).containsEntry("columnas", 16);
    }
}
