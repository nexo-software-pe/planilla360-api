package pe.nexosoftware.planilla360.compartido.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Respuestas 401/403 de la capa de seguridad en formato {@code application/problem+json}. */
@Component
class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper mapper;

    ProblemDetailSecurityHandler(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escribir(request, response, HttpStatus.UNAUTHORIZED, "No autenticado",
                "Debes iniciar sesión para acceder a este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN, "Acceso prohibido",
                "No tienes permisos para realizar esta operación.");
    }

    private void escribir(
            HttpServletRequest request, HttpServletResponse response, HttpStatus estado, String titulo,
            String detalle) throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "about:blank");
        cuerpo.put("title", titulo);
        cuerpo.put("status", estado.value());
        cuerpo.put("detail", detalle);
        cuerpo.put("instance", request.getRequestURI());
        response.setStatus(estado.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(cuerpo));
    }
}
