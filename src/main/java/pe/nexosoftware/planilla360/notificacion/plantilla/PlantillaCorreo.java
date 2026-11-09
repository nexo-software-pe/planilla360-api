package pe.nexosoftware.planilla360.notificacion.plantilla;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;

/** Plantilla de correo HTML con marcadores {@code {{nombre}}}; los valores se escapan para evitar HTML inyectado. */
public final class PlantillaCorreo {

    private final String contenido;

    private PlantillaCorreo(String contenido) {
        this.contenido = contenido;
    }

    public static PlantillaCorreo cargar(String ruta) {
        try {
            return new PlantillaCorreo(
                    new ClassPathResource(ruta).getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la plantilla " + ruta, e);
        }
    }

    public String renderizar(Map<String, String> valores) {
        String resultado = contenido;
        for (Map.Entry<String, String> valor : valores.entrySet()) {
            resultado = resultado.replace("{{" + valor.getKey() + "}}", HtmlUtils.htmlEscape(valor.getValue()));
        }
        return resultado;
    }
}
