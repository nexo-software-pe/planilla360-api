package pe.nexosoftware.planilla360.arquitectura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las variables que comparten Docker Compose y la API en el host llevan el mismo valor por defecto en
 * {@code .env.example}, {@code compose.yaml} y la configuración del perfil local. Así copiar
 * {@code .env.example} entero no rompe la autenticación de la API local. No usa Docker ni arranca Spring.
 */
class ConfiguracionLocalTests {

  /** Pareja (variable que lee compose, variable que lee la API). Misma variable en ambas si coincide el nombre. */
  private record Pareja(String compose, String api) {}

  private static final List<Pareja> PAREJAS_DIRECTAS =
      List.of(
          new Pareja("POSTGRES_USER", "DB_USER"),
          new Pareja("POSTGRES_PASSWORD", "DB_PASSWORD"),
          new Pareja("RABBITMQ_USER", "RABBITMQ_USER"),
          new Pareja("RABBITMQ_PASSWORD", "RABBITMQ_PASSWORD"),
          new Pareja("MINIO_ROOT_USER", "STORAGE_ACCESS_KEY"),
          new Pareja("MINIO_ROOT_PASSWORD", "STORAGE_SECRET_KEY"));

  private static final Pattern PLACEHOLDER_API = Pattern.compile("\\$\\{(\\w+):([^}]*)}");
  private static final Pattern PLACEHOLDER_COMPOSE = Pattern.compile("\\$\\{(\\w+):-([^}]*)}");
  private static final Pattern URL_DB = Pattern.compile("^jdbc:postgresql://[^:/]+:(\\d+)/(\\w+)$");

  private final Map<String, String> env = leerEnv(Path.of(".env.example"));
  private final Map<String, List<Aparicion>> compose =
      reunir(leer(Path.of("compose.yaml")), PLACEHOLDER_COMPOSE, "compose.yaml");
  private final Map<String, List<Aparicion>> api = leerApi();

  @Test
  @DisplayName("Las parejas usuario/clave de compose y la API tienen el mismo valor en las tres fuentes")
  void parejasDeCredencialesAlineadas() {
    for (Pareja p : PAREJAS_DIRECTAS) {
      String esperado = valor(compose, p.compose());
      assertThat(env.get(p.compose()))
          .as(mensaje(p.compose(), ".env.example", esperado, "compose.yaml"))
          .isEqualTo(esperado);
      assertThat(env.get(p.api()))
          .as(mensaje(p.api(), ".env.example", esperado, "compose.yaml"))
          .isEqualTo(esperado);
      assertThat(valor(api, p.api()))
          .as(mensaje(p.api(), "la configuración de la API", esperado, "compose.yaml"))
          .isEqualTo(esperado);
    }
  }

  @Test
  @DisplayName("POSTGRES_PORT y POSTGRES_DB coinciden con el puerto y el nombre de DB_URL")
  void puertoYNombreDeBaseAlineados() {
    String puerto = valor(compose, "POSTGRES_PORT");
    String base = valor(compose, "POSTGRES_DB");

    Matcher enEnv = urlDb(env.get("DB_URL"), ".env.example");
    Matcher enApi = urlDb(valor(api, "DB_URL"), "application-local.yml");

    assertThat(env.get("POSTGRES_PORT")).as(mensaje("POSTGRES_PORT", ".env.example", puerto, "compose.yaml")).isEqualTo(puerto);
    assertThat(env.get("POSTGRES_DB")).as(mensaje("POSTGRES_DB", ".env.example", base, "compose.yaml")).isEqualTo(base);
    assertThat(enEnv.group(1)).as(mensaje("el puerto de DB_URL", ".env.example", puerto, "compose.yaml")).isEqualTo(puerto);
    assertThat(enEnv.group(2)).as(mensaje("la base de DB_URL", ".env.example", base, "compose.yaml")).isEqualTo(base);
    assertThat(enApi.group(1)).as(mensaje("el puerto de DB_URL", "application-local.yml", puerto, "compose.yaml")).isEqualTo(puerto);
    assertThat(enApi.group(2)).as(mensaje("la base de DB_URL", "application-local.yml", base, "compose.yaml")).isEqualTo(base);
  }

  @Test
  @DisplayName("Una variable con dos valores por defecto distintos en el mismo archivo hace fallar el check")
  void variableConValoresDistintosEnUnArchivoFalla() {
    String contenido =
        """
        postgres:
          POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-planilla360}
        api:
          DB_PASSWORD: ${POSTGRES_PASSWORD:-otra-clave}
        """;

    assertThatThrownBy(
            () ->
                valorUnico(
                    reunir(contenido, PLACEHOLDER_COMPOSE, "compose.yaml"), "POSTGRES_PASSWORD"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("POSTGRES_PASSWORD")
        .hasMessageContaining("compose.yaml")
        .hasMessageContaining("línea 2")
        .hasMessageContaining("línea 4")
        .hasMessageContaining("planilla360")
        .hasMessageContaining("otra-clave");
  }

  @Test
  @DisplayName("Varias apariciones con el mismo valor por defecto devuelven ese valor")
  void variableConElMismoValorEnVariasApariciones() {
    String contenido = "a: ${X:-uno}\nb: ${X:-uno}\n";

    assertThat(valorUnico(reunir(contenido, PLACEHOLDER_COMPOSE, "compose.yaml"), "X"))
        .isEqualTo("uno");
  }

  @Test
  @DisplayName("Las líneas comentadas no cuentan como apariciones")
  void lineasComentadasSeIgnoran() {
    String contenido = "# ejemplo: ${X:-distinto}\n  # otro ${X:-raro}\nb: ${X:-bueno}\n";

    assertThat(valorUnico(reunir(contenido, PLACEHOLDER_COMPOSE, "compose.yaml"), "X"))
        .isEqualTo("bueno");
  }

  @Test
  @DisplayName("METRICS_PASSWORD coincide en .env.example, application-local.yml y la clave de Prometheus")
  void claveDeMetricasAlineada() {
    String clavePrometheus = leer(Path.of("docker/prometheus/metrics-password")).strip();

    assertThat(env.get("METRICS_PASSWORD"))
        .as(mensaje("METRICS_PASSWORD", ".env.example", clavePrometheus, "docker/prometheus/metrics-password"))
        .isEqualTo(clavePrometheus);
    assertThat(valor(api, "METRICS_PASSWORD"))
        .as(mensaje("METRICS_PASSWORD", "application-local.yml", clavePrometheus, "docker/prometheus/metrics-password"))
        .isEqualTo(clavePrometheus);
  }

  @Test
  @DisplayName("El mensaje de desalineación nombra la fuente de referencia que se le pasa")
  void mensajeNombraLaFuenteDeReferencia() {
    assertThat(mensaje("METRICS_PASSWORD", ".env.example", "clave", "docker/prometheus/metrics-password"))
        .contains("METRICS_PASSWORD en .env.example debe valer «clave»")
        .contains("el valor de referencia en docker/prometheus/metrics-password")
        .doesNotContain("valor por defecto de compose.yaml");
  }

  @Test
  @DisplayName("El mensaje de valores distintos nombra el archivo de cada aparición")
  void valoresDistintosNombranElArchivoDeLaAparicion() {
    Map<String, List<Aparicion>> apariciones =
        reunir("a: ${X:uno}\nb: ${X:dos}\n", PLACEHOLDER_API, "application-local.yml");

    assertThatThrownBy(() -> valorUnico(apariciones, "X"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("application-local.yml")
        .hasMessageContaining("línea 1")
        .hasMessageContaining("línea 2")
        .hasMessageNotContaining("application.yml");
  }

  @Test
  @DisplayName("Una variable ausente hace fallar el check y el mensaje nombra la fuente")
  void variableAusenteNombraLaFuente() {
    Map<String, List<Aparicion>> apariciones =
        reunir("a: ${X:-uno}\n", PLACEHOLDER_COMPOSE, "compose.yaml");

    assertThatThrownBy(() -> valorUnico(apariciones, "compose.yaml", "AUSENTE"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("compose.yaml")
        .hasMessageContaining("AUSENTE");
  }

  /** Una aparición de un valor por defecto: su valor, la línea (desde 1) y el archivo donde está. */
  private record Aparicion(String valor, int linea, String archivo) {}

  /** Reúne todas las apariciones de cada variable, ignorando las líneas comentadas con #. */
  private static Map<String, List<Aparicion>> reunir(
      String contenido, Pattern patron, String archivo) {
    Map<String, List<Aparicion>> resultado = new HashMap<>();
    String[] lineas = contenido.split("\\R");
    for (int i = 0; i < lineas.length; i++) {
      if (lineas[i].strip().startsWith("#")) {
        continue;
      }
      Matcher m = patron.matcher(lineas[i]);
      while (m.find()) {
        resultado
            .computeIfAbsent(m.group(1), k -> new ArrayList<>())
            .add(new Aparicion(m.group(2), i + 1, archivo));
      }
    }
    return resultado;
  }

  /** Exige que todas las apariciones de la variable tengan el mismo valor por defecto y lo devuelve. */
  private static String valorUnico(
      Map<String, List<Aparicion>> apariciones, String variable) {
    List<Aparicion> lista = apariciones.get(variable);
    assertThat(lista)
        .as("No hay un valor por defecto declarado para %s", variable)
        .isNotNull()
        .isNotEmpty();
    long distintos = lista.stream().map(Aparicion::valor).distinct().count();
    assertThat(distintos)
        .as(
            "Hay valores por defecto distintos para %s: %s. Deben ser el mismo en todas las apariciones",
            variable,
            lista.stream()
                .map(a -> "«" + a.valor() + "» (" + a.archivo() + ", línea " + a.linea() + ")")
                .collect(Collectors.joining(", ")))
        .isEqualTo(1L);
    return lista.get(0).valor();
  }

  private static Matcher urlDb(String url, String fuente) {
    Matcher m = URL_DB.matcher(String.valueOf(url));
    assertThat(m.matches())
        .as("DB_URL en %s debe tener la forma jdbc:postgresql://host:puerto/base y era: %s", fuente, url)
        .isTrue();
    return m;
  }

  private static String mensaje(
      String variable, String fuente, String esperado, String referencia) {
    return "%s en %s debe valer «%s» (el valor de referencia en %s). Si cambias un valor por defecto,"
            .formatted(variable, fuente, esperado, referencia)
        + " alinéalo en todas las fuentes que lo comparten (.env.example, compose.yaml,"
        + " application-local.yml, docker/prometheus/metrics-password)";
  }

  private static String valorUnico(
      Map<String, List<Aparicion>> apariciones, String fuente, String variable) {
    return valorUnico(apariciones, variable);
  }

  private static String valor(Map<String, List<Aparicion>> fuente, String variable) {
    return valorUnico(fuente, variable);
  }

  /** Valores por defecto de la API: el perfil local manda; application.yml completa lo que el local no redefine. */
  private static Map<String, List<Aparicion>> leerApi() {
    Map<String, List<Aparicion>> base =
        reunir(
            leer(Path.of("src/main/resources/application.yml")), PLACEHOLDER_API, "application.yml");
    base.putAll(reunir(
            leer(Path.of("src/main/resources/application-local.yml")),
            PLACEHOLDER_API,
            "application-local.yml"));
    return base;
  }

  private static Map<String, String> leerEnv(Path archivo) {
    Map<String, String> valores = new HashMap<>();
    for (String linea : leer(archivo).split("\\R")) {
      String t = linea.strip();
      int igual = t.indexOf('=');
      if (!t.startsWith("#") && igual > 0) {
        valores.put(t.substring(0, igual).strip(), t.substring(igual + 1).strip());
      }
    }
    return valores;
  }

  private static String leer(Path archivo) {
    try {
      return Files.readString(archivo, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo leer " + archivo, e);
    }
  }
}
