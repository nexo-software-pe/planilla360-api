package pe.nexosoftware.planilla360.compartido.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.soporte.IntegracionBase;

/**
 * El contrato público de la API vive versionado en {@code docs/openapi/openapi.json}. Esta prueba exige que coincida
 * con lo que la aplicación publica de verdad: cualquier cambio en un endpoint, un parámetro o un DTO obliga a
 * actualizar el archivo en el mismo PR, y ese cambio queda a la vista de la revisión. El CI compara además ese
 * archivo con el de la rama destino y falla si el cambio rompe la compatibilidad de la versión vigente
 * (ver {@code docs/contrato-api.md}).
 *
 * <p>Para regenerarlo tras un cambio intencional:
 * {@code ./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true}.
 */
@DisplayName("Contrato OpenAPI")
class ContratoOpenApiTests extends IntegracionBase {

    private static final Path CONTRATO = Path.of("docs", "openapi", "openapi.json");

    @Test
    void elContratoVersionadoCoincideConLaApiPublicada() throws Exception {
        String publicado = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Object actual = normalizar(json.readValue(publicado, Object.class));

        if (Boolean.getBoolean("contrato.actualizar")) {
            escribir(actual);
        }

        assertThat(Files.exists(CONTRATO))
                .as("No existe %s. Genéralo con: ./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true",
                        CONTRATO)
                .isTrue();
        Object versionado = normalizar(json.readValue(Files.readString(CONTRATO, StandardCharsets.UTF_8), Object.class));
        assertThat(actual)
                .as("La API publicada ya no coincide con %s. Si el cambio es intencional, regenera el contrato con "
                        + "./mvnw test -Dtest=ContratoOpenApiTests -Dcontrato.actualizar=true y revisa el diff: "
                        + "un cambio incompatible en /api/v1 debe ir en una versión nueva (/api/v2).", CONTRATO)
                .isEqualTo(versionado);
    }

    @Test
    void laInformacionDelContratoIdentificaLaVersionDeLaApi() throws Exception {
        String publicado = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(publicado).path("info").path("version").asString()).isEqualTo("v1");
    }

    private void escribir(Object contrato) throws IOException {
        Files.createDirectories(CONTRATO.getParent());
        String texto = json.writerWithDefaultPrettyPrinter().writeValueAsString(contrato)
                .replace("\r\n", "\n") + "\n";
        Files.writeString(CONTRATO, texto, StandardCharsets.UTF_8);
    }

    /** Ordena las claves de los objetos y quita lo que depende del entorno (la URL del servidor). */
    private static Object normalizar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> ordenado = new TreeMap<>();
            mapa.forEach((clave, v) -> {
                if (!"servers".equals(clave)) {
                    ordenado.put(String.valueOf(clave), normalizar(v));
                }
            });
            return ordenado;
        }
        if (valor instanceof List<?> lista) {
            List<Object> copia = new ArrayList<>();
            lista.forEach(v -> copia.add(normalizar(v)));
            return copia;
        }
        return valor;
    }
}
