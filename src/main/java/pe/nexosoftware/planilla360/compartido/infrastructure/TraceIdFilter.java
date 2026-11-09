package pe.nexosoftware.planilla360.compartido.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna un {@code traceId} a cada petición: queda en el MDC (aparece en todas las líneas de log de esa petición) y
 * se devuelve en la cabecera {@code X-Trace-Id} para que soporte pueda buscarlo en los logs.
 *
 * <p>Si el cliente (o un proxy) ya envía un identificador válido se respeta: el {@code trace-id} de la cabecera
 * W3C {@code traceparent} o, en su defecto, {@code X-Request-Id}. Lo que no cumple el formato se descarta y se genera
 * uno nuevo, para que nadie pueda inyectar texto arbitrario en los logs. Corre antes que la cadena de seguridad, de
 * modo que también quedan trazadas las respuestas 401 y 403.
 *
 * <p>Al terminar deja una línea de acceso por petición (método, ruta, estado y duración). No registra la cadena de
 * consulta ni cabeceras, porque pueden llevar datos personales. Las sondas de salud y el raspado de métricas, que
 * ocurren cada pocos segundos, se registran solo en nivel DEBUG.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TraceIdFilter.class);

    static final String CLAVE_MDC = "traceId";
    static final String CABECERA_RESPUESTA = "X-Trace-Id";

    private static final Pattern TRACEPARENT =
            Pattern.compile("^[0-9a-f]{2}-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}$");
    private static final Pattern ID_DE_PETICION = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final String SIN_TRACE = "00000000000000000000000000000000";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = identificadorDe(request);
        MDC.put(CLAVE_MDC, traceId);
        response.setHeader(CABECERA_RESPUESTA, traceId);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            registrarAcceso(request, response, (System.nanoTime() - inicio) / 1_000_000);
            MDC.remove(CLAVE_MDC);
        }
    }

    private static void registrarAcceso(HttpServletRequest request, HttpServletResponse response, long milisegundos) {
        boolean ruido = request.getRequestURI().startsWith("/actuator");
        if (ruido ? LOG.isDebugEnabled() : LOG.isInfoEnabled()) {
            String linea = "{} {} -> {} ({} ms)";
            Object[] datos = {request.getMethod(), request.getRequestURI(), response.getStatus(), milisegundos};
            if (ruido) {
                LOG.debug(linea, datos);
            } else {
                LOG.info(linea, datos);
            }
        }
    }

    private static String identificadorDe(HttpServletRequest request) {
        String traceparent = request.getHeader("traceparent");
        if (traceparent != null) {
            Matcher coincidencia = TRACEPARENT.matcher(traceparent.trim());
            if (coincidencia.matches() && !SIN_TRACE.equals(coincidencia.group(1))) {
                return coincidencia.group(1);
            }
        }
        String idDePeticion = request.getHeader("X-Request-Id");
        if (idDePeticion != null && ID_DE_PETICION.matcher(idDePeticion).matches()) {
            return idDePeticion;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
