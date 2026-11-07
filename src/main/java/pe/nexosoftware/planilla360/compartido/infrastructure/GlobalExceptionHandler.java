package pe.nexosoftware.planilla360.compartido.infrastructure;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pe.nexosoftware.planilla360.compartido.excepcion.DemasiadosIntentosException;
import pe.nexosoftware.planilla360.compartido.excepcion.PlanillaException;

/** Traduce todas las excepciones a {@code application/problem+json} (RFC 7807). */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PlanillaException.class)
    ResponseEntity<ProblemDetail> negocio(PlanillaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.estado(), ex.getMessage());
        problema.setTitle(ex.titulo());
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(ex.estado());
        if (ex instanceof DemasiadosIntentosException intentos) {
            respuesta.header(HttpHeaders.RETRY_AFTER, String.valueOf(intentos.reintentarEnSegundos()));
        }
        return respuesta.body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> accesoDenegado(AccessDeniedException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta operación.");
        problema.setTitle("Acceso prohibido");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problema);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> restricciones(ConstraintViolationException ex) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parámetros inválidos.");
        problema.setTitle("Datos inválidos");
        problema.setProperty("errores", ex.getConstraintViolations().stream()
                .map(v -> Map.of("campo", v.getPropertyPath().toString(), "mensaje", v.getMessage()))
                .toList());
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ProblemDetail> propiedadInvalida(PropertyReferenceException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "El campo de ordenamiento '" + ex.getPropertyName() + "' no existe.");
        problema.setTitle("Parámetro inválido");
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridad(DataIntegrityViolationException ex) {
        LOG.warn("Violación de integridad de datos", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes.");
        problema.setTitle("Conflicto con el estado actual");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        LOG.error("Error no controlado", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta nuevamente.");
        problema.setTitle("Error interno");
        return ResponseEntity.internalServerError().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.");
        problema.setTitle("Datos inválidos");
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of(
                        "campo", e.getField(),
                        "mensaje", Objects.requireNonNullElse(e.getDefaultMessage(), "valor inválido")))
                .toList();
        problema.setProperty("errores", errores);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }
}
